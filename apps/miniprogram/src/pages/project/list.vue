<script setup lang="ts">
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { ProjectStatus, ProjectVO } from '@ceams/types'
import { formatDate } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { projectStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

type TagType = 'pending' | 'approved' | 'rejected' | 'verified' | 'paid' | 'neutral'

/** 项目状态 -> 状态标签语义（仅用于展示配色） */
const projectStatusTagTypes: Record<ProjectStatus, TagType> = {
  PLANNING: 'neutral',
  IN_PROGRESS: 'verified',
  PAUSED: 'pending',
  COMPLETED: 'approved',
  CLOSED: 'neutral'
}

const list = ref<ProjectVO[]>([])
const keyword = ref('')
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const size = 20

async function load(reset = false): Promise<void> {
  if (reset) page.value = 1
  if (loading.value) return
  loading.value = true
  try {
    const res = await apiClient.project.projectPage({
      page: page.value,
      size,
      keyword: keyword.value || undefined
    })
    list.value = reset ? res.records : list.value.concat(res.records)
    total.value = res.total
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载项目失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  void load(true)
}

function openDetail(id: string): void {
  uni.navigateTo({ url: `/pages/project/detail?id=${id}` })
}

onShow(() => {
  void load(true)
})

onReachBottom(() => {
  if (loading.value || list.value.length >= total.value) return
  page.value += 1
  void load()
})
</script>

<template>
  <view class="ceams-page project-list">
    <view class="project-list__search">
      <input
        v-model="keyword"
        class="project-list__input"
        type="text"
        placeholder="项目名称 / 客户"
        @confirm="onSearch"
      />
      <button class="project-list__search-btn" @click="onSearch">查询</button>
    </view>

    <view class="project-list__summary">
      <view class="project-list__summary-cell">
        <text class="project-list__summary-label">项目总数</text>
        <text class="project-list__summary-value">{{ total }}</text>
      </view>
      <view class="project-list__summary-divider" />
      <view class="project-list__summary-cell">
        <text class="project-list__summary-label">当前展示</text>
        <text class="project-list__summary-value">{{ list.length }}</text>
      </view>
    </view>

    <view v-if="loading && list.length === 0" class="project-list__state">加载中...</view>
    <empty-state
      v-else-if="list.length === 0"
      title="暂无项目"
      description="调整关键词后重新查询"
    />

    <view
      v-for="item in list"
      :key="item.id"
      class="project-list__item"
      @click="openDetail(item.id)"
    >
      <list-card
        :title="item.projectName"
        :subtitle="'客户：' + (item.clientName || '-') + ' · 负责人：' + (item.managerName || '-') + ' · ' + (item.startDate ? formatDate(item.startDate) : '日期待定')"
      >
        <template #right>
          <status-tag
            :label="projectStatusLabels[item.status]"
            :type="projectStatusTagTypes[item.status]"
          />
        </template>
        <view class="project-list__metrics">
          <view class="project-list__metric">
            <text class="project-list__metric-label">合同金额</text>
            <amount-text :value="item.contractAmount ?? '--'" size="md" />
          </view>
          <view class="project-list__metric project-list__metric--spaced">
            <text class="project-list__metric-label">实际金额</text>
            <amount-text
              :value="item.actualAmount ?? '--'"
              size="md"
              color="var(--ceams-text-secondary)"
            />
          </view>
        </view>
      </list-card>
    </view>

    <view v-if="list.length > 0 && list.length < total" class="project-list__loadmore">
      上拉加载更多
    </view>
  </view>
</template>

<style scoped>
.project-list__search {
  display: flex;
  align-items: center;
  margin-bottom: var(--ceams-space-md);
}

.project-list__input {
  flex: 1;
  height: var(--ceams-control-height-sm);
  padding: 0 var(--ceams-space-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  background-color: var(--ceams-bg-card);
  border: var(--ceams-border-width) solid var(--ceams-border);
  border-radius: var(--ceams-radius-md);
  box-sizing: border-box;
}

.project-list__search-btn {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  height: var(--ceams-control-height-sm);
  margin-left: var(--ceams-space-sm);
  padding: 0 var(--ceams-space-lg);
  color: var(--ceams-text-inverse);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
  background-color: var(--ceams-color-primary);
  border-radius: var(--ceams-radius-md);
  box-sizing: border-box;
}

.project-list__summary {
  display: flex;
  align-items: center;
  margin-bottom: var(--ceams-space-md);
  padding: var(--ceams-space-md) var(--ceams-space-lg);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-card);
  box-shadow: var(--ceams-shadow-card);
}

.project-list__summary-cell {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.project-list__summary-label {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.project-list__summary-value {
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-title);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
  letter-spacing: var(--ceams-letter-spacing-number);
}

.project-list__summary-divider {
  align-self: stretch;
  width: var(--ceams-border-width);
  margin: 0 var(--ceams-space-lg);
  background-color: var(--ceams-divider);
}

.project-list__state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-body);
  text-align: center;
}

.project-list__metrics {
  padding-top: var(--ceams-space-sm);
  border-top: var(--ceams-border-width) solid var(--ceams-divider);
}

/* 金额行：标签在左、金额右对齐，便于纵向快速比对 */
.project-list__metric {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.project-list__metric--spaced {
  margin-top: var(--ceams-space-xs);
}

.project-list__metric-label {
  flex-shrink: 0;
  margin-right: var(--ceams-space-md);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
}

.project-list__loadmore {
  padding: var(--ceams-space-md) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}
</style>
