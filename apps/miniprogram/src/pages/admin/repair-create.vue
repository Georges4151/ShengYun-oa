<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { RepairCategory } from '@ceams/types'
import { chooseImage, uploadFile } from '../../platform/upload'
import { previewLocalImages } from '../../platform/image'
import { ATTACHMENT_UPLOAD_URL } from '../../platform/config'
import { getAccessToken } from '../../platform/auth'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'
import { repairCategoryLabels } from '../../utils/labels'

interface ValueEvent {
  detail?: { value?: string }
}

function eventValue(event: unknown): string {
  const detail = (event as ValueEvent).detail
  return detail && typeof detail.value === 'string' ? detail.value : ''
}

const CATEGORY_VALUES: RepairCategory[] = ['ELECTRIC', 'WATER', 'FURNITURE', 'IT', 'OTHER']
const CATEGORY_OPTIONS = CATEGORY_VALUES.map((value) => repairCategoryLabels[value])
const MAX_IMAGES = 6

interface RepairImage {
  local: string
  name: string
}

const form = reactive({
  categoryIndex: 0,
  location: '',
  description: ''
})
const images = ref<RepairImage[]>([])
const submitting = ref(false)
const uploading = ref(false)

const category = computed<RepairCategory>(() => CATEGORY_VALUES[form.categoryIndex] ?? 'OTHER')

async function pickImages(): Promise<void> {
  const remaining = MAX_IMAGES - images.value.length
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
      // 后端 images 字段存储文件服务文件名，取上传路径最后一段
      const name = url.split('/').pop() ?? ''
      if (name) {
        images.value.push({ local: path, name })
      }
    }
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '图片上传失败'), icon: 'none' })
  } finally {
    uploading.value = false
  }
}

function removeImage(index: number): void {
  images.value.splice(index, 1)
}

/** 点击缩略图预览已选现场图片（本地临时路径，无需鉴权） */
function previewImage(index: number): void {
  const urls = images.value.map((item) => item.local)
  previewLocalImages(urls, images.value[index]?.local ?? '').catch((err: unknown) => {
    uni.showToast({ title: apiErrorMessage(err, '预览图片失败'), icon: 'none' })
  })
}

async function submit(): Promise<void> {
  if (!form.location.trim()) {
    uni.showToast({ title: '请输入报修位置', icon: 'none' })
    return
  }
  if (!form.description.trim()) {
    uni.showToast({ title: '请填写问题描述', icon: 'none' })
    return
  }
  // 现场图片为必传，至少 1 张
  if (images.value.length === 0) {
    uni.showToast({ title: '请至少上传 1 张现场图片', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const repairId = await apiClient.admin.repairCreate({
      location: form.location.trim(),
      category: category.value,
      description: form.description.trim(),
      images: images.value.map((item) => item.name)
    })
    // 创建接口仅返回主键，回查我的报修以展示工单号
    let repairNo = ''
    try {
      const repairs = await apiClient.admin.myRepairs()
      repairNo = repairs.find((item) => item.id === repairId)?.repairNo ?? ''
    } catch {
      // 工单号回查失败不影响提交结果
    }
    uni.showToast({ title: repairNo ? `工单号 ${repairNo}` : '报修已提交', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack({ delta: 1 })
    }, 900)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '提交报修失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="ceams-page">
    <view class="ceams-card">
      <view class="field">
        <text class="field__label">报修分类</text>
        <picker
          mode="selector"
          :range="CATEGORY_OPTIONS"
          :value="form.categoryIndex"
          @change="form.categoryIndex = Number(eventValue($event))"
        >
          <view class="field__value">{{ CATEGORY_OPTIONS[form.categoryIndex] }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="field__label">报修位置</text>
        <input
          class="field__input"
          :value="form.location"
          placeholder="如：A 座 3 层 302 会议室"
          @input="form.location = eventValue($event)"
        />
      </view>

      <view class="field field--column">
        <text class="field__label">问题描述</text>
        <textarea
          class="field__textarea"
          :value="form.description"
          placeholder="请描述故障现象、影响范围等"
          maxlength="500"
          @input="form.description = eventValue($event)"
        />
      </view>

      <view class="field field--column">
        <text class="field__label">现场图片（必传，最多 {{ MAX_IMAGES }} 张）</text>
        <view class="images">
          <view v-for="(item, index) in images" :key="item.name" class="images__item">
            <image class="images__thumb" :src="item.local" mode="aspectFill" @click="previewImage(index)" />
            <text class="images__remove" @click.stop="removeImage(index)">删除</text>
          </view>
          <view v-if="images.length < MAX_IMAGES" class="images__add" @click="pickImages">
            <text class="images__add-text">{{ uploading ? '上传中...' : '+ 添加' }}</text>
          </view>
        </view>
      </view>
    </view>

    <button
      class="ceams-btn ceams-btn--primary submit"
      :disabled="submitting || uploading || images.length === 0"
      @click="submit"
    >
      {{ submitting ? '提交中...' : '提交报修' }}
    </button>
  </view>
</template>

<style scoped>
.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-sm) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.field--column {
  flex-direction: column;
  align-items: flex-start;
}

.field__label {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.field__value {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.field__input {
  flex: 1;
  margin-left: var(--ceams-space-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  text-align: right;
}

.field__textarea {
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

.images {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ceams-space-sm);
  margin-top: var(--ceams-space-xs);
}

.images__item {
  position: relative;
  width: 160rpx;
  height: 160rpx;
}

.images__thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: var(--ceams-radius-md);
}

.images__remove {
  position: absolute;
  right: 0;
  bottom: 0;
  padding: 0 var(--ceams-space-2xs);
  background-color: rgba(0, 0, 0, 0.55);
  border-radius: var(--ceams-radius-md);
  color: #ffffff;
  font-size: var(--ceams-font-size-mini);
}

.images__add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 160rpx;
  height: 160rpx;
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
}

.images__add-text {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.submit {
  margin-top: var(--ceams-space-lg);
}
</style>
