<script setup lang="ts">
/**
 * 首页 · 移动工作台
 * 结构：品牌问候区（含今日打卡状态卡）→ 四张指标卡 → 常用功能宫格 → 行政服务宫格。
 * 取数：bpm 待办/超时/我发起、notification 未读数、hr 当月打卡记录。
 */
import { computed, ref } from 'vue'
import { onShow, onUnload } from '@dcloudio/uni-app'
import type { CheckinVO } from '@ceams/types'
import { formatDate, formatDateTime } from '@ceams/utils'
import { clearSession, getUser, getUserName, isLoggedIn, patchUser, rolesOf } from '../../platform/auth'
import { chooseAndUploadAvatar } from '../../platform/avatar'
import { API_BASE_URL } from '../../platform/config'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'

/** 图标色板：与 theme.css 的 --ceams-icon-bg-* / --ceams-icon-fg-* 一一对应 */
type IconTone = 'blue' | 'green' | 'orange' | 'purple' | 'teal' | 'pink' | 'indigo' | 'red'

/**
 * app-icon 把颜色写进 SVG data URI，无法解析 CSS 变量，
 * 因此这里用常量镜像令牌的 foreground 值，保证与设计令牌一致。
 */
const TONE_COLORS: Record<IconTone, string> = {
  blue: '#2563eb',
  green: '#059669',
  orange: '#ea580c',
  purple: '#7c3aed',
  teal: '#0d9488',
  pink: '#db2777',
  indigo: '#4f46e5',
  red: '#dc2626'
}

/** 功能宫格入口 */
interface MenuItem {
  title: string
  path: string
  /** app-icon 图标名 */
  icon: string
  /** 图标底块色板 */
  tone: IconTone
}

/** 指标卡：value 为 null 表示加载中 */
interface MetricCard {
  key: string
  label: string
  icon: string
  tone: IconTone
  value: number | null
  path: string
  /** 是否需要警示色（超时类指标） */
  danger: boolean
}

const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const name = ref('')
/** 行政服务入口仅 SYS_ADMIN 可见（角色来自登录会话） */
const canAdmin = ref(false)
/** 当前登录用户头像：后端返回的相对路径或完整 URL，空串表示未设置。 */
const avatar = ref('')
/** 头像上传中标记，避免重复触发。 */
const uploadingAvatar = ref(false)
/** 页面是否仍存活：卸载后丢弃异步响应，避免对已销毁页面赋值。 */
let alive = true
/** 当前时间：用于问候语与日期展示，onShow 时刷新 */
const now = ref(new Date())

/* ---------------- 今日打卡状态 ---------------- */
const todayInTime = ref('')
const todayOutTime = ref('')
const checkinLoading = ref(false)

/* ---------------- 四项指标 ---------------- */
const pendingTotal = ref<number | null>(null)
const overdueTotal = ref<number | null>(null)
const unreadTotal = ref<number | null>(null)
const initiatedTotal = ref<number | null>(null)
const metricsLoading = ref(false)

/** 头像可被 <image> 直接加载的完整地址。 */
const avatarSrc = computed<string>(() => {
  const value = avatar.value
  if (!value) return ''
  return value.startsWith('http') ? value : `${API_BASE_URL}${value}`
})

/** 无头像时展示的用户名首字，兜底为「用」。 */
const avatarText = computed<string>(() => name.value.slice(0, 1) || '用')

/** 按小时给出问候语：上午 / 下午 / 晚上 */
const greeting = computed<string>(() => {
  const hour = now.value.getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

/** 问候语 + 姓名，姓名为空时只保留问候语 */
const greetingText = computed<string>(() =>
  name.value ? `${greeting.value}，${name.value}` : greeting.value
)

/** 今日日期：如「11月19日 周三」 */
const dateLabel = computed<string>(
  () => `${now.value.getMonth() + 1}月${now.value.getDate()}日 ${weekdays[now.value.getDay()] ?? ''}`
)

/** 打卡状态文案：优先展示签退，其次打卡，均无则为未打卡 */
const checkinText = computed<string>(() => {
  if (todayOutTime.value) return `今日已签退 ${todayOutTime.value}`
  if (todayInTime.value) return `今日已打卡 ${todayInTime.value}`
  return '今日还未打卡'
})

/** 四张指标卡：接口失败统一降级为 0，加载中为 null（展示「-」） */
const metricCards = computed<MetricCard[]>(() => [
  {
    key: 'pending',
    label: '我的待办',
    icon: 'todo',
    tone: 'blue',
    value: pendingTotal.value,
    path: '/pages/bpm/todo?tab=pending',
    danger: false
  },
  {
    key: 'overdue',
    label: '超时待办',
    icon: 'warning',
    tone: 'red',
    // 待办页 onLoad 仅识别 tab/taskId/instanceId，不支持 overdue 参数，故只带页签
    value: overdueTotal.value,
    path: '/pages/bpm/todo?tab=pending',
    danger: true
  },
  {
    key: 'unread',
    label: '未读通知',
    icon: 'notification',
    tone: 'orange',
    value: unreadTotal.value,
    path: '/pages/notification/list',
    danger: false
  },
  {
    key: 'initiated',
    label: '我发起的在办',
    icon: 'doc',
    tone: 'green',
    value: initiatedTotal.value,
    path: '/pages/bpm/todo?tab=initiated',
    danger: false
  }
])

/** 常用功能宫格（「我的待办」已由上方指标卡承载，此处不再重复） */
const menus: MenuItem[] = [
  { title: '项目与进度款', path: '/pages/project/list', icon: 'project', tone: 'blue' },
  { title: '发起报销', path: '/pages/expense/create', icon: 'expense', tone: 'orange' },
  { title: '打卡', path: '/pages/hr/checkin', icon: 'checkin', tone: 'green' },
  { title: '请假申请', path: '/pages/hr/leave-create', icon: 'leave', tone: 'pink' },
  { title: '我的档案', path: '/pages/hr/profile', icon: 'profile', tone: 'blue' },
  { title: '工资条', path: '/pages/hr/salary-slip', icon: 'salary', tone: 'pink' },
  { title: '绩效自评', path: '/pages/hr/performance-self', icon: 'performance', tone: 'purple' },
  { title: '项目变更', path: '/pages/project/change-create', icon: 'change', tone: 'indigo' },
  { title: '只读账目', path: '/pages/payment/list', icon: 'payment', tone: 'teal' },
  { title: '通知列表', path: '/pages/notification/list', icon: 'notification', tone: 'orange' },
  { title: '知识库', path: '/pages/doc/list', icon: 'doc', tone: 'blue' },
  { title: '智能助手', path: '/pages/ai/assistant', icon: 'ai', tone: 'purple' }
]

/** 行政服务入口：资产、用车、会议室、报修申请、我的报修、访客核销 */
const adminMenus: MenuItem[] = [
  { title: '资产领用', path: '/pages/admin/asset-apply', icon: 'asset', tone: 'teal' },
  { title: '用车申请', path: '/pages/admin/vehicle-apply', icon: 'vehicle', tone: 'indigo' },
  { title: '会议室预定', path: '/pages/admin/meeting-apply', icon: 'meeting', tone: 'purple' },
  { title: '报修申请', path: '/pages/admin/repair-create', icon: 'repair', tone: 'indigo' },
  { title: '我的报修', path: '/pages/admin/repair-list', icon: 'repair', tone: 'orange' },
  { title: '访客核销', path: '/pages/admin/visitor-checkin', icon: 'visitor', tone: 'orange' }
]

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

/** 当月 YYYY-MM：与 hr/checkin 页取数口径一致 */
function monthOf(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
}

/** 打卡时间只保留 HH:mm */
function timeOf(value: string): string {
  return formatDateTime(value).slice(11, 16)
}

/** 加载今日打卡状态：失败或未绑定员工档案时静默降级为「今日还未打卡」 */
async function loadCheckinStatus(): Promise<void> {
  if (checkinLoading.value) return
  checkinLoading.value = true
  try {
    const list: CheckinVO[] = await apiClient.hr.myCheckins(monthOf(now.value))
    if (!alive) return
    const todayKey = formatDate(now.value)
    const todayList = list.filter((item) => item.checkinDate === todayKey)
    const inItem = todayList.find((item) => item.checkinType === 'IN')
    const outItem = todayList.find((item) => item.checkinType === 'OUT')
    todayInTime.value = inItem ? timeOf(inItem.checkinTime) : ''
    todayOutTime.value = outItem ? timeOf(outItem.checkinTime) : ''
  } catch {
    // 网络异常、未绑定员工档案等场景静默处理，不打扰用户
    if (!alive) return
    todayInTime.value = ''
    todayOutTime.value = ''
  } finally {
    // 页面已卸载时不再写状态
    if (alive) checkinLoading.value = false
  }
}

/** 并发加载四项指标：单个失败只影响自身，失败显示 0 且不弹窗 */
async function loadMetrics(): Promise<void> {
  if (metricsLoading.value) return
  metricsLoading.value = true
  pendingTotal.value = null
  overdueTotal.value = null
  unreadTotal.value = null
  initiatedTotal.value = null
  try {
    const [pending, overdue, unread, initiated] = await Promise.allSettled([
      apiClient.bpm.pendingTasks({ page: 1, size: 1 }),
      apiClient.bpm.pendingTasks({ page: 1, size: 1, overdue: true }),
      apiClient.notification.unreadCount(),
      apiClient.bpm.initiated({ page: 1, size: 1, status: 'RUNNING' })
    ])
    if (!alive) return
    pendingTotal.value = pending.status === 'fulfilled' ? pending.value.total : 0
    overdueTotal.value = overdue.status === 'fulfilled' ? overdue.value.total : 0
    unreadTotal.value = unread.status === 'fulfilled' ? unread.value : 0
    initiatedTotal.value = initiated.status === 'fulfilled' ? initiated.value.total : 0
  } finally {
    // 页面已卸载时不再写状态
    if (alive) metricsLoading.value = false
  }
}

onShow(() => {
  alive = true
  if (!isLoggedIn()) {
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  now.value = new Date()
  name.value = getUserName()
  canAdmin.value = rolesOf().includes('SYS_ADMIN')
  // 每次显示首页时同步本地缓存头像，保证其他端修改后即时生效
  avatar.value = getUser()?.avatar ?? ''
  // 返回首页时刷新打卡与指标，两个请求并行且各自防重
  void Promise.all([loadCheckinStatus(), loadMetrics()])
})

onUnload(() => {
  alive = false
})

/**
 * 点击首页头像：选择图片并上传头像。
 * 未绑定员工档案的账号看不到「我的档案」，因此首页提供同一个换头像入口。
 */
async function onChangeAvatar(): Promise<void> {
  // 上传中忽略重复点击，避免并发上传产生错乱
  if (uploadingAvatar.value) return
  uploadingAvatar.value = true
  try {
    const url = await chooseAndUploadAvatar()
    if (!url) return
    avatar.value = url
    patchUser({ avatar: url })
    uni.showToast({ title: '头像已更新', icon: 'success' })
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '头像更新失败'), icon: 'none' })
  } finally {
    uploadingAvatar.value = false
  }
}

function open(path: string): void {
  uni.navigateTo({ url: path })
}

function logout(): void {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        clearSession()
        uni.reLaunch({ url: '/pages/login/login' })
      }
    }
  })
}
</script>

<template>
  <view class="home">
    <!-- 品牌区：渐变底 + 底部大圆角，内嵌压边的今日打卡状态卡 -->
    <view class="home__banner">
      <view class="home__banner-bar">
        <view class="home__brand">
          <text class="home__greeting">{{ greetingText }}</text>
          <text class="home__subtitle">CEAMS 建筑财务移动办公</text>
          <text class="home__date">{{ dateLabel }}</text>
        </view>
        <!-- 右侧操作区：头像入口 + 退出登录，保持横向一行 -->
        <view class="home__actions">
          <view class="home__avatar" @tap="onChangeAvatar">
            <image v-if="avatarSrc" class="home__avatar-img" :src="avatarSrc" mode="aspectFill" />
            <text v-else class="home__avatar-text">{{ avatarText }}</text>
          </view>
          <button class="home__logout" @click="logout">退出登录</button>
        </view>
      </view>

      <!-- 今日打卡状态：负 margin 上移，压在品牌区下沿 -->
      <view class="home__checkin">
        <view class="home__checkin-main">
          <view class="home__checkin-icon">
            <app-icon name="checkin" :size="40" :color="TONE_COLORS.green" />
          </view>
          <view class="home__checkin-info">
            <text class="home__checkin-label">今日打卡</text>
            <text class="home__checkin-status">{{ checkinText }}</text>
          </view>
        </view>
        <button class="home__checkin-btn" @click="open('/pages/hr/checkin')">去打卡</button>
      </view>
    </view>

    <view class="home__body">
      <!-- 四张指标卡：2 列负 margin 网格 -->
      <view class="home__grid">
        <view v-for="card in metricCards" :key="card.key" class="home__cell">
          <view
            class="home__metric"
            hover-class="home__metric--pressed"
            @click="open(card.path)"
          >
            <view class="home__metric-top">
              <view :class="['home__tone', 'home__metric-icon', 'home__tone--' + card.tone]">
                <app-icon :name="card.icon" :size="36" :color="TONE_COLORS[card.tone]" />
              </view>
              <text
                :class="[
                  'home__metric-value',
                  card.danger ? 'home__metric-value--danger' : ''
                ]"
              >
                {{ card.value === null ? '-' : card.value }}
              </text>
            </view>
            <text class="home__metric-label">{{ card.label }}</text>
          </view>
        </view>
      </view>

      <!-- 常用功能：4 列宫格 -->
      <view class="home__section">
        <view class="home__section-bar"></view>
        <text class="home__section-title">常用功能</text>
      </view>

      <view class="home__grid">
        <view v-for="item in menus" :key="item.path" class="home__cell home__cell--quarter">
          <view class="home__entry" hover-class="home__entry--pressed" @click="open(item.path)">
            <view :class="['home__tone', 'home__entry-icon', 'home__tone--' + item.tone]">
              <app-icon :name="item.icon" :size="44" :color="TONE_COLORS[item.tone]" />
            </view>
            <text class="home__entry-title">{{ item.title }}</text>
          </view>
        </view>
      </view>

      <!-- 行政服务：仅 SYS_ADMIN 可见，同样 4 列宫格 -->
      <template v-if="canAdmin">
        <view class="home__section home__section--spaced">
          <view class="home__section-bar"></view>
          <text class="home__section-title">行政服务</text>
        </view>

        <view class="home__grid">
          <view v-for="item in adminMenus" :key="item.path" class="home__cell home__cell--quarter">
            <view class="home__entry" hover-class="home__entry--pressed" @click="open(item.path)">
              <view :class="['home__tone', 'home__entry-icon', 'home__tone--' + item.tone]">
                <app-icon :name="item.icon" :size="44" :color="TONE_COLORS[item.tone]" />
              </view>
              <text class="home__entry-title">{{ item.title }}</text>
            </view>
          </view>
        </view>
      </template>

      <text class="home__footer">数据来自 CEAMS 服务端 · v1.0.0</text>
    </view>
  </view>
</template>

<style scoped>
.home {
  min-height: 100vh;
  box-sizing: border-box;
  background-color: var(--ceams-bg-page);
}

/* ---------------- 顶部品牌区 ---------------- */
.home__banner {
  /* 底部不留白：由打卡卡片的负 margin 制造压边效果 */
  padding: var(--ceams-space-2xl) var(--ceams-space-xl) 0;
  background-image: var(--ceams-gradient-brand);
  border-bottom-left-radius: var(--ceams-radius-xl);
  border-bottom-right-radius: var(--ceams-radius-xl);
  box-sizing: border-box;
}

.home__banner-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.home__brand {
  flex: 1;
  min-width: 0;
}

.home__greeting {
  display: block;
  color: var(--ceams-text-inverse);
  font-size: var(--ceams-font-size-title);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
}

.home__subtitle {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-inverse);
  opacity: 0.72;
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.home__date {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-inverse);
  opacity: 0.6;
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

/* 右侧操作区：头像与退出按钮同一行，垂直居中 */
.home__actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: var(--ceams-space-md);
}

/* 首页头像入口：白色圆形底 + 半透明描边，未设置头像时展示姓名首字 */
.home__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  overflow: hidden;
  background-color: var(--ceams-bg-card);
  border: 2rpx solid rgba(255, 255, 255, 0.6);
  box-sizing: border-box;
}

.home__avatar-img {
  width: 100%;
  height: 100%;
}

.home__avatar-text {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
}

.home__logout {
  flex-shrink: 0;
  margin: 0 0 0 var(--ceams-space-md);
  padding: 0;
  background-color: transparent;
  color: var(--ceams-text-inverse);
  opacity: 0.78;
  font-size: var(--ceams-font-size-caption);
  font-weight: var(--ceams-font-weight-regular);
  line-height: var(--ceams-line-height-normal);
}

.home__logout::after {
  border: none;
}

/* ---------------- 今日打卡状态卡 ---------------- */
.home__checkin {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: var(--ceams-space-lg) 0 -60rpx;
  padding: var(--ceams-space-md) var(--ceams-space-lg);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-card);
  box-shadow: var(--ceams-shadow-raised);
  box-sizing: border-box;
}

.home__checkin-main {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

/* 圆形图标底块：绿色系，语义为考勤 */
.home__checkin-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background-color: var(--ceams-icon-bg-green);
}

.home__checkin-info {
  flex: 1;
  min-width: 0;
  margin-left: var(--ceams-space-sm);
}

.home__checkin-label {
  display: block;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.home__checkin-status {
  display: block;
  margin-top: 2rpx;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.home__checkin-btn {
  flex-shrink: 0;
  height: 56rpx;
  margin: 0 0 0 var(--ceams-space-sm);
  padding: 0 var(--ceams-space-lg);
  border-radius: var(--ceams-radius-pill);
  background-color: var(--ceams-color-primary-tint);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-mini);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: 56rpx;
}

.home__checkin-btn::after {
  border: none;
}

/* ---------------- 内容区 ---------------- */
.home__body {
  /* 上内边距预留打卡卡片压边的 60rpx */
  padding: 100rpx var(--ceams-space-xl) var(--ceams-space-2xl);
  box-sizing: border-box;
}

/* 负 margin 网格：兼容不支持 gap 的旧基础库 */
.home__grid {
  display: flex;
  flex-wrap: wrap;
  margin: -8rpx;
}

.home__cell {
  width: 50%;
  padding: 8rpx;
  box-sizing: border-box;
}

.home__cell--quarter {
  width: 25%;
}

/* ---------------- 指标卡 ---------------- */
.home__metric {
  padding: var(--ceams-space-lg) var(--ceams-space-md);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-card);
  box-shadow: var(--ceams-shadow-card);
  box-sizing: border-box;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.home__metric--pressed {
  background-color: var(--ceams-bg-subtle);
  transform: scale(0.97);
}

.home__metric-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

/* 指标卡左上角图标底块 */
.home__metric-icon {
  width: var(--ceams-control-height-sm);
  height: var(--ceams-control-height-sm);
  border-radius: var(--ceams-radius-md);
}

.home__metric-value {
  color: var(--ceams-text-amount);
  font-size: var(--ceams-font-size-amount-lg);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
  letter-spacing: var(--ceams-letter-spacing-number);
}

/* 超时指标：使用警示色强调 */
.home__metric-value--danger {
  color: var(--ceams-color-danger);
}

.home__metric-label {
  display: block;
  margin-top: var(--ceams-space-sm);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

/* ---------------- 分区标题 ---------------- */
.home__section {
  display: flex;
  align-items: center;
  margin: var(--ceams-space-lg) 0 var(--ceams-space-md);
}

.home__section-bar {
  width: var(--ceams-space-2xs);
  height: var(--ceams-space-lg);
  margin-right: var(--ceams-space-xs);
  background-color: var(--ceams-color-primary);
  border-radius: var(--ceams-radius-xs);
}

.home__section-title {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.home__section--spaced {
  margin-top: var(--ceams-space-xl);
}

/* ---------------- 功能宫格 ---------------- */
.home__entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--ceams-space-md) var(--ceams-space-2xs);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-card);
  box-shadow: var(--ceams-shadow-card);
  box-sizing: border-box;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.home__entry--pressed {
  background-color: var(--ceams-bg-subtle);
  transform: scale(0.97);
}

.home__entry-icon {
  width: var(--ceams-control-height-sm);
  height: var(--ceams-control-height-sm);
  border-radius: var(--ceams-radius-md);
}

.home__entry-title {
  display: -webkit-box;
  /* 固定两行高度，保证单行/双行标题的格子高度一致 */
  min-height: 72rpx;
  margin-top: var(--ceams-space-xs);
  overflow: hidden;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
  text-align: center;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

/* ---------------- 图标色板底块 ---------------- */
.home__tone {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.home__tone--blue {
  background-color: var(--ceams-icon-bg-blue);
}

.home__tone--green {
  background-color: var(--ceams-icon-bg-green);
}

.home__tone--orange {
  background-color: var(--ceams-icon-bg-orange);
}

.home__tone--purple {
  background-color: var(--ceams-icon-bg-purple);
}

.home__tone--teal {
  background-color: var(--ceams-icon-bg-teal);
}

.home__tone--pink {
  background-color: var(--ceams-icon-bg-pink);
}

.home__tone--indigo {
  background-color: var(--ceams-icon-bg-indigo);
}

/* 警示色板：仅用于超时待办 */
.home__tone--red {
  background-color: var(--ceams-color-danger-tint);
}

/* ---------------- 页脚 ---------------- */
.home__footer {
  display: block;
  margin-top: var(--ceams-space-xl);
  text-align: center;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}
</style>
