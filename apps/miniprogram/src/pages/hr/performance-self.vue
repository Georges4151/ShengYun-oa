<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type {
  PerformanceItem,
  PerformanceReviewVO,
  PerformanceScoreItem
} from '@ceams/types'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'
import { hrPerformanceReviewStatusLabels } from '../../utils/labels'

interface UniInputEvent extends Event {
  detail?: { value?: string }
}

/**
 * 兼容 H5 与小程序事件结构：小程序事件取 detail.value，H5 回退到 target.value。
 */
function inputValue(event: Event): string {
  const detail = (event as UniInputEvent).detail
  if (detail && typeof detail.value === 'string') return detail.value
  const target = event.target as HTMLInputElement | HTMLTextAreaElement | null
  return target?.value ?? ''
}

const loading = ref(false)
const submitting = ref(false)
const reviews = ref<PerformanceReviewVO[]>([])
const activeId = ref('')
/** 考评项名称 → 分数输入值（保持字符串，避免浮点运算） */
const scores = ref<Record<string, string>>({})
const comment = ref('')

const activeReview = computed<PerformanceReviewVO | null>(
  () => reviews.value.find((item) => item.id === activeId.value) ?? null
)

/** 优先取考评单返回的模板指标快照；缺失时回退到已存在的自评明细。 */
const activeItems = computed<PerformanceItem[]>(() => {
  const review = activeReview.value
  if (!review) return []
  if (review.templateItems?.length) return review.templateItems
  return (review.selfItems ?? []).map<PerformanceItem>((item) => ({
    name: item.name,
    weight: item.weight,
    minScore: null,
    maxScore: null,
    description: null
  }))
})

const weightSum = computed<number>(() =>
  activeItems.value.reduce((sum, item) => sum + (Number(item.weight) || 0), 0)
)

function selectReview(review: PerformanceReviewVO): void {
  activeId.value = review.id
  const next: Record<string, string> = {}
  for (const item of review.selfItems ?? []) {
    next[item.name] = item.score
  }
  scores.value = next
  comment.value = review.selfComment ?? ''
}

/** 仅本人 DRAFT 考评单可自评；进入页面即刷新。 */
async function load(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    const res = await apiClient.hr.myPerformanceReviews({ page: 1, size: 50 })
    reviews.value = res.records.filter((item) => item.status === 'DRAFT')
    if (reviews.value.length > 0) {
      const exists = reviews.value.some((item) => item.id === activeId.value)
      selectReview(exists ? reviews.value.find((item) => item.id === activeId.value)! : reviews.value[0])
    } else {
      activeId.value = ''
      scores.value = {}
      comment.value = ''
    }
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载绩效考评单失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function onScoreInput(name: string, event: Event): void {
  scores.value = { ...scores.value, [name]: inputValue(event) }
}

function onCommentInput(event: Event): void {
  comment.value = inputValue(event)
}

async function submit(): Promise<void> {
  const review = activeReview.value
  if (!review) return
  if (activeItems.value.length === 0) {
    uni.showToast({ title: '未能获取绩效指标，请联系 HR', icon: 'none' })
    return
  }

  const items: PerformanceScoreItem[] = []
  for (const item of activeItems.value) {
    const raw = String(scores.value[item.name] ?? '').trim()
    if (!/^\d+$/.test(raw)) {
      uni.showToast({ title: `请为「${item.name}」填写 0~100 的整数分`, icon: 'none' })
      return
    }
    const value = Number(raw)
    if (value < 0 || value > 100) {
      uni.showToast({ title: `「${item.name}」分数需在 0~100 之间`, icon: 'none' })
      return
    }
    items.push({ name: item.name, weight: item.weight, score: raw })
  }

  submitting.value = true
  try {
    await apiClient.hr.performanceSelf(review.id, {
      items,
      comment: comment.value.trim() || undefined
    })
    uni.showToast({ title: '自评已提交', icon: 'success' })
    await load()
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '提交自评失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  void load()
})
</script>

<template>
  <view class="ceams-page">
    <view v-if="loading && reviews.length === 0" class="state">加载中...</view>

    <empty-state
      v-if="!loading && reviews.length === 0"
      title="暂无待自评的绩效"
      description="HR 生成考评单后，可在此填写本月绩效自评"
      icon="★"
    />

    <template v-if="activeReview">
      <!-- 多张待自评考评单时提供切换 -->
      <view v-if="reviews.length > 1" class="chips">
        <view
          v-for="item in reviews"
          :key="item.id"
          class="chip"
          :class="{ 'chip--active': item.id === activeId }"
          @click="selectReview(item)"
        >
          <text>{{ item.period }} · {{ item.templateName }}</text>
        </view>
      </view>

      <view class="ceams-card">
        <view class="head">
          <view class="head__main">
            <text class="head__title">{{ activeReview.templateName }}</text>
            <text class="head__sub">
              周期 {{ activeReview.period }} · 指标 {{ activeItems.length }} 项 · 权重合计 {{ weightSum }}
            </text>
          </view>
          <status-tag
            :label="hrPerformanceReviewStatusLabels[activeReview.status]"
            type="pending"
          />
        </view>

        <view v-for="(item, index) in activeItems" :key="index" class="item">
          <view class="item__main">
            <text class="item__name">{{ item.name }}</text>
            <text class="item__meta">
              权重 {{ item.weight }}<text v-if="item.description"> · {{ item.description }}</text>
            </text>
          </view>
          <input
            class="item__input"
            type="number"
            :value="scores[item.name] ?? ''"
            placeholder="0-100"
            @input="onScoreInput(item.name, $event)"
          />
        </view>

        <view v-if="activeItems.length === 0" class="hint">
          <text class="hint__text">未能加载绩效指标，请联系 HR 检查模板配置。</text>
        </view>
      </view>

      <view class="ceams-card">
        <text class="section-title">自评评语</text>
        <textarea
          class="comment"
          :value="comment"
          placeholder="请输入自评评语（可选）"
          maxlength="500"
          @input="onCommentInput"
        />
      </view>

      <view class="submit">
        <button
          class="ceams-btn ceams-btn--primary"
          :disabled="submitting || activeItems.length === 0"
          @click="submit"
        >
          {{ submitting ? '提交中...' : '提交自评' }}
        </button>
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

.chips {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: var(--ceams-space-md);
}

.chip {
  margin: 0 var(--ceams-space-xs) var(--ceams-space-xs) 0;
  padding: var(--ceams-space-2xs) var(--ceams-space-sm);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-pill);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  box-shadow: var(--ceams-shadow-card);
}

.chip--active {
  background-color: var(--ceams-color-primary-tint);
  color: var(--ceams-color-primary);
  font-weight: var(--ceams-font-weight-semibold);
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-xs);
}

.head__main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.head__title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.head__sub {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-sm) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.item:last-child {
  border-bottom: none;
}

.item__main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.item__name {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.item__meta {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.item__input {
  width: 120px;
  height: var(--ceams-control-height-sm);
  padding: 0 var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  text-align: center;
  box-sizing: border-box;
}

.hint {
  margin-top: var(--ceams-space-sm);
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
}

.hint__text {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.section-title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.comment {
  width: 100%;
  min-height: 120px;
  padding: var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  box-sizing: border-box;
}

.submit {
  margin-top: var(--ceams-space-lg);
}
</style>
