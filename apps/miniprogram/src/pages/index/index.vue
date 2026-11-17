<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { clearSession, getUserName, isLoggedIn } from '../../platform/auth'

interface MenuItem {
  title: string
  path: string
  /** 纯 CSS 图标块中的几何字符，不引入图标库 */
  icon: string
  /** 卡片下方的一行业务说明 */
  desc: string
}

const name = ref('')
const menus: MenuItem[] = [
  {
    title: '项目与进度款',
    path: '/pages/project/list',
    icon: '▤',
    desc: '查看项目进度与进度款'
  },
  {
    title: '我的待办',
    path: '/pages/expense/pending',
    icon: '✓',
    desc: '处理报销与变更审批'
  },
  {
    title: '发起报销',
    path: '/pages/expense/create',
    icon: '¥',
    desc: '填写报销单并上传票据'
  },
  {
    title: '打卡',
    path: '/pages/hr/checkin',
    icon: '◷',
    desc: '定位打卡与补卡申请'
  },
  {
    title: '请假申请',
    path: '/pages/hr/leave-create',
    icon: '◑',
    desc: '提交请假进入审批'
  },
  {
    title: '我的档案',
    path: '/pages/hr/profile',
    icon: '☰',
    desc: '档案、合同与假期额度'
  },
  {
    title: '工资条',
    path: '/pages/hr/salary-slip',
    icon: '¥',
    desc: '查看每月实发与明细'
  },
  {
    title: '绩效自评',
    path: '/pages/hr/performance-self',
    icon: '★',
    desc: '填写本月绩效自评'
  },
  {
    title: '项目变更',
    path: '/pages/project/change-create',
    icon: '⇄',
    desc: '发起项目变更审批'
  },
  {
    title: '只读账目',
    path: '/pages/payment/list',
    icon: '◈',
    desc: '浏览付款与账目明细'
  },
  {
    title: '通知列表',
    path: '/pages/notification/list',
    icon: '▣',
    desc: '接收系统与审批通知'
  },
  {
    title: '智能助手',
    path: '/pages/ai/assistant',
    icon: '✦',
    desc: '用自然语言查账与解释待办'
  }
]

/** 行政服务入口：资产、用车、会议室、报修、访客核销 */
const adminMenus: MenuItem[] = [
  {
    title: '资产领用',
    path: '/pages/admin/asset-apply',
    icon: '▢',
    desc: '申请领用办公资产'
  },
  {
    title: '用车申请',
    path: '/pages/admin/vehicle-apply',
    icon: '◎',
    desc: '申请公务用车'
  },
  {
    title: '会议室预定',
    path: '/pages/admin/meeting-apply',
    icon: '◫',
    desc: '预定会议室并查看占用'
  },
  {
    title: '报修申请',
    path: '/pages/admin/repair-create',
    icon: '✚',
    desc: '提交设施报修工单'
  },
  {
    title: '访客核销',
    path: '/pages/admin/visitor-checkin',
    icon: '⌗',
    desc: '扫描访客二维码核销'
  }
]

onShow(() => {
  if (!isLoggedIn()) {
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  name.value = getUserName()
})

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
    <!-- 品牌区：主色渐变 + 底部大圆角 -->
    <view class="home__banner">
      <view class="home__banner-bar">
        <view class="home__brand">
          <text class="home__greeting">{{ name ? '你好，' + name : '你好，欢迎回来' }}</text>
          <text class="home__subtitle">CEAMS 建筑财务移动办公</text>
        </view>
        <button class="home__logout" @click="logout">退出登录</button>
      </view>
    </view>

    <view class="home__body">
      <view class="home__section">
        <view class="home__section-bar"></view>
        <text class="home__section-title">常用功能</text>
      </view>

      <!-- 2 列网格：宽度百分比 + margin 实现，不使用 gap -->
      <view class="home__menu">
        <view
          v-for="item in menus"
          :key="item.path"
          class="home__card"
          hover-class="home__card--pressed"
          @click="open(item.path)"
        >
          <view class="home__icon">
            <text class="home__icon-symbol">{{ item.icon }}</text>
          </view>
          <view class="home__card-text">
            <text class="home__card-title">{{ item.title }}</text>
            <text class="home__card-desc ceams-ellipsis">{{ item.desc }}</text>
          </view>
        </view>
      </view>

      <view class="home__section home__section--spaced">
        <view class="home__section-bar"></view>
        <text class="home__section-title">行政服务</text>
      </view>

      <view class="home__menu">
        <view
          v-for="item in adminMenus"
          :key="item.path"
          class="home__card"
          hover-class="home__card--pressed"
          @click="open(item.path)"
        >
          <view class="home__icon">
            <text class="home__icon-symbol">{{ item.icon }}</text>
          </view>
          <view class="home__card-text">
            <text class="home__card-title">{{ item.title }}</text>
            <text class="home__card-desc ceams-ellipsis">{{ item.desc }}</text>
          </view>
        </view>
      </view>

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
  padding: var(--ceams-space-2xl) var(--ceams-space-xl);
  background-image: linear-gradient(
    135deg,
    var(--ceams-color-primary) 0%,
    var(--ceams-color-primary-deep) 100%
  );
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

/* ---------------- 内容区 ---------------- */
.home__body {
  padding: var(--ceams-space-lg) var(--ceams-space-xl) var(--ceams-space-2xl);
  box-sizing: border-box;
}

.home__section {
  display: flex;
  align-items: center;
  margin-bottom: var(--ceams-space-md);
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
  margin-top: var(--ceams-space-lg);
}

/* ---------------- 功能入口卡片（2 列） ---------------- */
.home__menu {
  display: flex;
  flex-wrap: wrap;
}

.home__card {
  display: flex;
  align-items: center;
  width: calc(50% - var(--ceams-space-xs));
  margin: 0 var(--ceams-space-xs) var(--ceams-space-md) 0;
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-card);
  box-shadow: var(--ceams-shadow-card);
  box-sizing: border-box;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.home__card--pressed {
  background-color: var(--ceams-bg-subtle);
  transform: scale(0.97);
}

.home__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: var(--ceams-control-height-sm);
  height: var(--ceams-control-height-sm);
  background-color: var(--ceams-color-primary-tint);
  border-radius: var(--ceams-radius-md);
}

.home__icon-symbol {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.home__card-text {
  flex: 1;
  min-width: 0;
  margin-left: var(--ceams-space-sm);
}

.home__card-title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.home__card-desc {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
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
