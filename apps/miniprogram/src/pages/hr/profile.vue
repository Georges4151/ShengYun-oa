<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type {
  CheckinSupplementVO,
  ContractVO,
  DeptVO,
  EmployeeVO,
  HrApprovalStatus,
  HrContractStatus,
  LeaveQuotaVO,
  LeaveRequestVO,
  PositionVO
} from '@ceams/types'
import { formatDate, formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { getUser, patchUser } from '../../platform/auth'
import { chooseAndUploadAvatar } from '../../platform/avatar'
import { API_BASE_URL } from '../../platform/config'
import { apiErrorMessage, isEmployeeUnbound } from '../../utils/error'
import {
  hrApprovalStatusLabels,
  hrCheckinTypeLabels,
  hrContractStatusLabels,
  hrContractTypeLabels,
  hrEmployeeStatusLabels,
  hrEmploymentTypeLabels,
  hrLeaveTypeLabels
} from '../../utils/labels'

type TagType = 'pending' | 'approved' | 'rejected' | 'neutral'

/** 审批状态 → 状态标签语义（仅用于展示配色） */
const approvalTagTypes: Record<HrApprovalStatus, TagType> = {
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
  WITHDRAWN: 'neutral',
  CANCELLED: 'neutral',
  TERMINATED: 'rejected'
}

const contractTagTypes: Record<HrContractStatus, TagType> = {
  ACTIVE: 'approved',
  EXPIRED: 'pending',
  TERMINATED: 'neutral'
}

const profile = ref<EmployeeVO | null>(null)
const contracts = ref<ContractVO[]>([])
const quotas = ref<LeaveQuotaVO[]>([])
const leaveRequests = ref<LeaveRequestVO[]>([])
const supplements = ref<CheckinSupplementVO[]>([])
const depts = ref<DeptVO[]>([])
const positions = ref<PositionVO[]>([])
const loading = ref(false)
const unbound = ref(false)

/** 当前登录用户头像：后端返回的相对路径或完整 URL，空串表示未设置。 */
const avatar = ref<string>(getUser()?.avatar ?? '')
/** 头像上传中标记，避免重复触发。 */
const uploadingAvatar = ref(false)

/** 头像可被 <image> 直接加载的完整地址。 */
const avatarSrc = computed<string>(() => {
  const value = avatar.value
  if (!value) return ''
  return value.startsWith('http') ? value : `${API_BASE_URL}${value}`
})

const currentYear = computed<number>(() => new Date().getFullYear())
const deptLabel = computed<string>(() => {
  const id = profile.value?.deptId
  if (!id) return '-'
  return depts.value.find((item) => item.id === id)?.name ?? id
})
const positionLabel = computed<string>(() => {
  const id = profile.value?.positionId
  if (!id) return '-'
  return positions.value.find((item) => item.id === id)?.name ?? id
})

/** 部门树拍平，便于按 ID 查找名称。 */
function flattenDepts(nodes: DeptVO[], result: DeptVO[] = []): DeptVO[] {
  for (const node of nodes) {
    result.push(node)
    if (node.children?.length) flattenDepts(node.children, result)
  }
  return result
}

/** 部门/岗位名称仅用于展示，无权限或接口异常时静默回退为原始 ID。 */
async function loadReferences(): Promise<void> {
  await Promise.allSettled([
    apiClient.hr.positionList().then((list) => {
      positions.value = list
    }),
    apiClient.iam.deptTree().then((tree) => {
      depts.value = flattenDepts(tree)
    })
  ])
}

/** 手机号兜底脱敏：后端已脱敏时原样展示，11 位明文仅保留首尾。 */
function maskPhone(value?: string | null): string {
  if (!value) return '-'
  return value.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}

function dateRange(start?: string | null, end?: string | null): string {
  return `${start ? formatDate(start) : '-'} ~ ${end ? formatDate(end) : '-'}`
}

async function load(): Promise<void> {
  if (loading.value) return
  loading.value = true
  unbound.value = false
  try {
    profile.value = await apiClient.hr.myProfile()
  } catch (err) {
    if (isEmployeeUnbound(err)) {
      unbound.value = true
      profile.value = null
      return
    }
    uni.showToast({ title: apiErrorMessage(err, '加载员工档案失败'), icon: 'none' })
    return
  } finally {
    loading.value = false
  }

  const me = profile.value
  if (!me) return
  const results = await Promise.allSettled([
    apiClient.hr
      .contractPage({ employeeId: me.id, size: 100 })
      .then((res) => {
        contracts.value = res.records
      }),
    apiClient.hr.leaveQuotaList({ employeeId: me.id, year: currentYear.value }).then((list) => {
      quotas.value = list
    }),
    apiClient.hr.myLeaveRequests().then((list) => {
      leaveRequests.value = list
    }),
    apiClient.hr.mySupplements().then((list) => {
      supplements.value = list
    })
  ])
  if (results.some((item) => item.status === 'rejected')) {
    uni.showToast({ title: '部分档案数据加载失败', icon: 'none' })
  }
}

/**
 * 点击头像 / 提示文字：选择图片并上传头像。
 * 选择与上传细节由 chooseAndUploadAvatar 统一处理：取消时静默结束，成功后同步本地缓存。
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

onShow(() => {
  // 每次显示页面时同步一次本地缓存头像，防止其他端修改后展示过期
  avatar.value = getUser()?.avatar ?? ''
  void load()
  void loadReferences()
})
</script>

<template>
  <view class="ceams-page">
    <view v-if="loading && !profile" class="state">加载中...</view>

    <empty-state
      v-if="unbound"
      title="未绑定员工档案"
      description="请联系 HR 绑定员工档案后查看个人信息"
    />

    <template v-if="profile">
      <view class="ceams-card">
        <view class="profile__head">
          <view class="profile__avatar-wrap">
            <image
              v-if="avatarSrc"
              class="profile__avatar"
              :src="avatarSrc"
              mode="aspectFill"
              @tap="onChangeAvatar"
            />
            <view
              v-else
              class="profile__avatar profile__avatar--empty"
              @tap="onChangeAvatar"
            >
              <text>{{ profile.name.slice(0, 1) }}</text>
            </view>
            <text class="profile__avatar-tip" @tap="onChangeAvatar">点击更换头像</text>
          </view>
          <view class="profile__main">
            <text class="profile__name">{{ profile.name }}</text>
            <text class="profile__no">工号 {{ profile.employeeNo }}</text>
          </view>
          <status-tag
            :label="hrEmployeeStatusLabels[profile.status]"
            :type="profile.status === 'TERMINATED' ? 'neutral' : 'approved'"
          />
        </view>

        <view class="ceams-list-item kv">
          <text class="kv__label">部门</text>
          <text class="kv__value">{{ deptLabel }}</text>
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">岗位</text>
          <text class="kv__value">{{ positionLabel }}</text>
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">手机号</text>
          <text class="kv__value">{{ maskPhone(profile.phone) }}</text>
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">入职日期</text>
          <text class="kv__value">{{ profile.hireDate ? formatDate(profile.hireDate) : '-' }}</text>
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">用工类型</text>
          <text class="kv__value">
            {{ profile.employmentType ? hrEmploymentTypeLabels[profile.employmentType] : '-' }}
          </text>
        </view>
        <view class="ceams-list-item kv kv--last">
          <text class="kv__label">状态</text>
          <text class="kv__value">{{ hrEmployeeStatusLabels[profile.status] }}</text>
        </view>
      </view>

      <view class="ceams-card">
        <view class="ceams-section-title section-title">我的合同</view>
        <text v-if="contracts.length === 0" class="empty-text">暂无合同记录</text>
        <view v-for="item in contracts" :key="item.id" class="row">
          <view class="row__main">
            <text class="row__title ceams-ellipsis">{{ item.contractNo }}</text>
            <text class="row__sub">
              {{ hrContractTypeLabels[item.contractType] }} ·
              {{ dateRange(item.startDate, item.endDate) }}
            </text>
          </view>
          <status-tag
            :label="hrContractStatusLabels[item.status]"
            :type="contractTagTypes[item.status]"
          />
        </view>
      </view>

      <view class="ceams-card">
        <view class="ceams-section-title section-title">{{ currentYear }} 年假期额度</view>
        <text v-if="quotas.length === 0" class="empty-text">暂无假期额度</text>
        <view v-for="item in quotas" :key="item.id" class="ceams-list-item kv">
          <text class="kv__label">{{ hrLeaveTypeLabels[item.leaveType] }}</text>
          <text class="kv__value">
            剩余 {{ item.remainingDays ?? '-' }} / 共 {{ item.totalDays }} 天
          </text>
        </view>
      </view>

      <view class="ceams-card">
        <view class="ceams-section-title section-title">我的请假</view>
        <text v-if="leaveRequests.length === 0" class="empty-text">暂无请假记录</text>
        <view v-for="item in leaveRequests" :key="item.id" class="row">
          <view class="row__main">
            <text class="row__title">
              {{ hrLeaveTypeLabels[item.leaveType] }} · {{ item.days ?? '-' }} 天
            </text>
            <text class="row__sub">{{ dateRange(item.startDate, item.endDate) }}</text>
          </view>
          <status-tag
            :label="hrApprovalStatusLabels[item.approvalStatus]"
            :type="approvalTagTypes[item.approvalStatus]"
          />
        </view>
      </view>

      <view class="ceams-card">
        <view class="ceams-section-title section-title">我的补卡</view>
        <text v-if="supplements.length === 0" class="empty-text">暂无补卡记录</text>
        <view v-for="item in supplements" :key="item.id" class="row">
          <view class="row__main">
            <text class="row__title">
              {{ item.checkinDate }} · {{ hrCheckinTypeLabels[item.checkinType] }}
            </text>
            <text class="row__sub">
              {{ formatDateTime(item.checkinTime) }}
              <text v-if="item.reason"> · {{ item.reason }}</text>
            </text>
          </view>
          <status-tag
            :label="hrApprovalStatusLabels[item.approvalStatus]"
            :type="approvalTagTypes[item.approvalStatus]"
          />
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped>
.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}

.profile__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-xs);
}

/* 头像 + 「点击更换头像」提示 */
.profile__avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  margin-right: var(--ceams-space-md);
}

/* 头像区与首页保持一致：白色圆底 + 细描边 */
.profile__avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: var(--ceams-radius-pill);
  overflow: hidden;
  background-color: var(--ceams-bg-card);
  border: 2rpx solid var(--ceams-border);
  box-sizing: border-box;
}

/* 无头像时的圆形占位，居中显示姓名首字 */
.profile__avatar--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--ceams-color-primary-tint);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-bold);
}

.profile__avatar-tip {
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-placeholder);
  font-size: 22rpx;
}

.profile__main {
  flex: 1;
  min-width: 0;
}

.profile__name {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-title);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
}

.profile__no {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.kv {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kv--last {
  border-bottom: none;
}

.kv__label {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.kv__value {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  text-align: right;
}

/* 分组标题沿用全局 .ceams-section-title，仅补下间距 */
.section-title {
  margin-bottom: var(--ceams-space-xs);
}

.empty-text {
  display: block;
  padding: var(--ceams-space-md) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-sm) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.row:last-child {
  border-bottom: none;
}

.row__main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.row__title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: 28rpx;
  font-weight: var(--ceams-font-weight-semibold);
}

.row__sub {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
  line-height: var(--ceams-line-height-normal);
}
</style>
