<script setup lang="ts">
/**
 * 报修工单详情：
 * - 展示工单基础信息、时间节点、现场图片与完工图片；
 * - 维修人 / SYS_ADMIN 在「处理中」状态可提交完工（处理说明 + 1～6 张完工图片）；
 * - 报修人在「已完工」状态可执行验收。
 * 远程图片读取需要鉴权，统一由 platform/image 下载后再展示或预览。
 */
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import type { RepairStatus, RepairVO } from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { chooseImage, uploadFile } from '../../platform/upload'
import { ATTACHMENT_UPLOAD_URL } from '../../platform/config'
import { getAccessToken, getUser, rolesOf } from '../../platform/auth'
import { previewLocalImages, previewRemoteImage, resolveRemoteImage } from '../../platform/image'
import { apiErrorMessage } from '../../utils/error'
import { repairCategoryLabels, repairStatusLabels } from '../../utils/labels'

type TagType = 'pending' | 'approved' | 'rejected' | 'verified' | 'paid' | 'neutral'

interface ValueEvent {
  detail?: { value?: string }
}

interface LocalImage {
  local: string
  name: string
}

/** 工单状态 -> 状态标签语义（仅用于展示配色） */
const statusTagTypes: Record<RepairStatus, TagType> = {
  SUBMITTED: 'pending',
  ASSIGNED: 'pending',
  PROCESSING: 'verified',
  DONE: 'approved',
  ACCEPTED: 'paid',
  REJECTED: 'rejected'
}

const MAX_IMAGES = 6

function eventValue(event: unknown): string {
  const detail = (event as ValueEvent).detail
  return detail && typeof detail.value === 'string' ? detail.value : ''
}

const id = ref('')
const detail = ref<RepairVO | null>(null)
const loading = ref(false)
/** 远程图片文件名 -> 本地临时路径，供缩略图与点击预览复用 */
const thumbUrls = ref<Record<string, string>>({})

/** 当前登录用户，用于判断完工 / 验收权限 */
const currentUserId = ref('')
const isAdmin = ref(false)

const finishRemark = ref('')
const finishImages = ref<LocalImage[]>([])
const uploading = ref(false)
const submitting = ref(false)
const accepting = ref(false)

/** 处理中且当前用户是维修人或系统管理员时可提交完工 */
const canFinish = computed<boolean>(() => {
  const data = detail.value
  if (!data || data.status !== 'PROCESSING') return false
  return isAdmin.value || (!!currentUserId.value && data.assigneeId === currentUserId.value)
})

/** 已完工且当前用户是报修人时可验收 */
const canAccept = computed<boolean>(() => {
  const data = detail.value
  if (!data || data.status !== 'DONE') return false
  return !!currentUserId.value && data.reporterId === currentUserId.value
})

/**
 * 预下载工单图片到本地临时文件，供 <image> 缩略图直接使用。
 * 单张失败不影响整体展示，失败项回落到占位块。
 */
async function loadThumbs(data: RepairVO): Promise<void> {
  const names = [...(data.images ?? []), ...(data.finishImages ?? [])]
  const next: Record<string, string> = {}
  for (const name of names) {
    const cached = thumbUrls.value[name]
    if (cached) {
      next[name] = cached
      continue
    }
    try {
      next[name] = await resolveRemoteImage(name)
    } catch {
      // 忽略单张下载失败，点击缩略图时仍会重新尝试并 toast
    }
  }
  thumbUrls.value = next
}

async function load(): Promise<void> {
  if (!id.value) return
  loading.value = true
  try {
    const data = await apiClient.admin.repairDetail(id.value)
    detail.value = data
    await loadThumbs(data)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载报修详情失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

/** 点击现场 / 完工图片：鉴权下载后调起预览 */
function openRemoteImage(name: string): void {
  previewRemoteImage(name).catch((err: unknown) => {
    uni.showToast({ title: apiErrorMessage(err, '预览图片失败'), icon: 'none' })
  })
}

/** 选择并上传完工图片：后端返回文件地址，取最后一段作为文件名 */
async function pickFinishImages(): Promise<void> {
  const remaining = MAX_IMAGES - finishImages.value.length
  if (remaining <= 0) {
    uni.showToast({ title: `最多上传 ${MAX_IMAGES} 张图片`, icon: 'none' })
    return
  }
  try {
    const paths = await chooseImage(remaining)
    if (paths.length === 0) return
    uploading.value = true
    const token = getAccessToken()
    const header = token ? { Authorization: `Bearer ${token}` } : undefined
    for (const path of paths) {
      const url = await uploadFile(ATTACHMENT_UPLOAD_URL, path, { header })
      const name = url.split('/').pop() ?? ''
      if (name) {
        finishImages.value.push({ local: path, name })
      }
    }
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '图片上传失败'), icon: 'none' })
  } finally {
    uploading.value = false
  }
}

function removeFinishImage(index: number): void {
  finishImages.value.splice(index, 1)
}

/** 预览已选完工图片（本地临时路径） */
function previewFinishImage(index: number): void {
  const urls = finishImages.value.map((item) => item.local)
  previewLocalImages(urls, finishImages.value[index]?.local ?? '').catch((err: unknown) => {
    uni.showToast({ title: apiErrorMessage(err, '预览图片失败'), icon: 'none' })
  })
}

async function submitFinish(): Promise<void> {
  if (submitting.value) return
  if (finishImages.value.length === 0) {
    uni.showToast({ title: '请至少上传 1 张完工图片', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await apiClient.admin.repairFinish(id.value, {
      remark: finishRemark.value.trim() || undefined,
      images: finishImages.value.map((item) => item.name)
    })
    finishImages.value = []
    finishRemark.value = ''
    uni.showToast({ title: '已提交完工', icon: 'success' })
    await load()
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '提交完工失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

async function acceptRepair(): Promise<void> {
  if (accepting.value) return
  accepting.value = true
  try {
    await apiClient.admin.repairAccept(id.value)
    uni.showToast({ title: '已验收', icon: 'success' })
    await load()
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '验收失败'), icon: 'none' })
  } finally {
    accepting.value = false
  }
}

onLoad((query) => {
  id.value = typeof query?.id === 'string' ? query.id : ''
  currentUserId.value = getUser()?.id ?? ''
  isAdmin.value = rolesOf().includes('SYS_ADMIN')
  void load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<template>
  <view class="ceams-page repair-detail">
    <view v-if="detail">
      <view class="head">
        <view class="head__main">
          <text class="head__label">工单号</text>
          <text class="head__no">{{ detail.repairNo }}</text>
        </view>
        <status-tag
          :label="repairStatusLabels[detail.status]"
          :type="statusTagTypes[detail.status]"
        />
      </view>

      <view class="ceams-card">
        <view class="section">
          <view class="section__bar"></view>
          <text class="section__title">工单信息</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">报修位置</text>
          <text class="row__value">{{ detail.location || '-' }}</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">报修分类</text>
          <text class="row__value">{{ repairCategoryLabels[detail.category] }}</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">问题描述</text>
          <text class="row__value">{{ detail.description || '-' }}</text>
        </view>
        <view class="ceams-list-item ceams-list-item--last row">
          <text class="row__label">处理备注</text>
          <text class="row__value">{{ detail.remark || '-' }}</text>
        </view>
      </view>

      <view class="ceams-card">
        <view class="section">
          <view class="section__bar"></view>
          <text class="section__title">时间节点</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">提交</text>
          <text class="row__value">已提交</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">派单</text>
          <text class="row__value">{{ formatDateTime(detail.assignedAt) }}</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">处理</text>
          <text class="row__value">{{ formatDateTime(detail.processedAt) }}</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">完工</text>
          <text class="row__value">{{ formatDateTime(detail.finishedAt) }}</text>
        </view>
        <view class="ceams-list-item ceams-list-item--last row">
          <text class="row__label">验收</text>
          <text class="row__value">{{ formatDateTime(detail.acceptedAt) }}</text>
        </view>
      </view>

      <view class="ceams-card">
        <view class="section">
          <view class="section__bar"></view>
          <text class="section__title">现场图片（{{ detail.images?.length ?? 0 }} 张）</text>
        </view>
        <view v-if="detail.images?.length" class="thumbs">
          <view
            v-for="name in detail.images"
            :key="name"
            class="thumb"
            @click="openRemoteImage(name)"
          >
            <image v-if="thumbUrls[name]" class="thumb__img" :src="thumbUrls[name]" mode="aspectFill" />
            <view v-else class="thumb__placeholder">
              <text class="thumb__placeholder-text">图</text>
            </view>
          </view>
        </view>
        <text v-else class="empty-text">-</text>
      </view>

      <view class="ceams-card">
        <view class="section">
          <view class="section__bar"></view>
          <text class="section__title">完工图片（{{ detail.finishImages?.length ?? 0 }} 张）</text>
        </view>
        <view v-if="detail.finishImages?.length" class="thumbs">
          <view
            v-for="name in detail.finishImages"
            :key="name"
            class="thumb"
            @click="openRemoteImage(name)"
          >
            <image v-if="thumbUrls[name]" class="thumb__img" :src="thumbUrls[name]" mode="aspectFill" />
            <view v-else class="thumb__placeholder">
              <text class="thumb__placeholder-text">图</text>
            </view>
          </view>
        </view>
        <text v-else class="empty-text">-</text>
      </view>

      <view v-if="canFinish" class="ceams-card">
        <view class="section">
          <view class="section__bar"></view>
          <text class="section__title">提交完工</text>
        </view>
        <text class="form__hint">请填写处理说明并上传完工图片（必传，最多 {{ MAX_IMAGES }} 张）</text>
        <textarea
          class="form__textarea"
          :value="finishRemark"
          placeholder="请描述处理过程、更换配件等"
          maxlength="500"
          @input="finishRemark = eventValue($event)"
        />
        <view class="thumbs">
          <view v-for="(item, index) in finishImages" :key="item.name" class="thumb">
            <image
              class="thumb__img"
              :src="item.local"
              mode="aspectFill"
              @click="previewFinishImage(index)"
            />
            <text class="thumb__remove" @click.stop="removeFinishImage(index)">删除</text>
          </view>
          <view v-if="finishImages.length < MAX_IMAGES" class="thumb thumb--add" @click="pickFinishImages">
            <text class="thumb__add-text">{{ uploading ? '上传中...' : '+ 添加' }}</text>
          </view>
        </view>
        <button
          class="ceams-btn ceams-btn--primary form__submit"
          :disabled="submitting || uploading || finishImages.length === 0"
          @click="submitFinish"
        >
          {{ submitting ? '提交中...' : '提交完工' }}
        </button>
      </view>

      <view v-if="canAccept" class="ceams-card">
        <button class="ceams-btn ceams-btn--primary" :disabled="accepting" @click="acceptRepair">
          {{ accepting ? '验收中...' : '验收' }}
        </button>
      </view>
    </view>

    <view v-else class="state">{{ loading ? '加载中...' : '暂无数据' }}</view>
  </view>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-md);
  padding: var(--ceams-space-lg);
  background-color: var(--ceams-color-primary-tint);
  border: var(--ceams-border-width) solid var(--ceams-color-primary-tint-strong);
  border-radius: var(--ceams-radius-card);
  box-sizing: border-box;
}

.head__main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.head__label {
  display: block;
  color: var(--ceams-color-primary-deep);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-tight);
  letter-spacing: var(--ceams-letter-spacing-number);
}

.head__no {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
}

.section {
  display: flex;
  align-items: center;
  margin-bottom: var(--ceams-space-xs);
}

.section__bar {
  width: 6rpx;
  height: var(--ceams-space-lg);
  margin-right: var(--ceams-space-xs);
  background-color: var(--ceams-color-primary);
  border-radius: var(--ceams-radius-xs);
}

.section__title {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.row__label {
  flex-shrink: 0;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.row__value {
  flex: 1;
  margin-left: var(--ceams-space-lg);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
  text-align: right;
  word-break: break-word;
}

.thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ceams-space-sm);
  margin-top: var(--ceams-space-xs);
}

.thumb {
  position: relative;
  width: 160rpx;
  height: 160rpx;
}

.thumb__img {
  width: 160rpx;
  height: 160rpx;
  border-radius: var(--ceams-radius-md);
}

.thumb__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 160rpx;
  height: 160rpx;
  background-color: var(--ceams-bg-subtle);
  border: var(--ceams-border-width) solid var(--ceams-divider);
  border-radius: var(--ceams-radius-md);
  box-sizing: border-box;
}

.thumb__placeholder-text {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
}

.thumb__remove {
  position: absolute;
  right: 0;
  bottom: 0;
  padding: 0 var(--ceams-space-2xs);
  background-color: rgba(0, 0, 0, 0.55);
  border-radius: var(--ceams-radius-md);
  color: #ffffff;
  font-size: var(--ceams-font-size-mini);
}

.thumb--add {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
}

.thumb__add-text {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.empty-text {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.form__hint {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.form__textarea {
  width: 100%;
  min-height: 160px;
  margin-top: var(--ceams-space-xs);
  padding: var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  box-sizing: border-box;
}

.form__submit {
  margin-top: var(--ceams-space-lg);
}

.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}
</style>
