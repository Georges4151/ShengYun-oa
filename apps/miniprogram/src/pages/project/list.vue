<script setup lang="ts">
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { ProjectVO } from '@ceams/types'
import { formatMoney } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { projectStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

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
  <view class="page">
    <view class="search">
      <input v-model="keyword" class="search__input" type="text" placeholder="项目名称 / 客户" />
      <button class="search__btn" @click="onSearch">查询</button>
    </view>

    <view v-if="loading && list.length === 0" class="empty">加载中...</view>
    <view v-else-if="list.length === 0" class="empty">暂无项目</view>

    <view v-for="item in list" :key="item.id" class="card" @click="openDetail(item.id)">
      <view class="card__row">
        <text class="card__title">{{ item.projectName }}</text>
        <text class="card__tag">{{ projectStatusLabels[item.status] }}</text>
      </view>
      <text class="card__line">客户：{{ item.clientName || '-' }}</text>
      <text class="card__line">类型：{{ item.projectType }}</text>
      <text class="card__line">合同金额：{{ formatMoney(item.contractAmount) }}</text>
    </view>

    <view v-if="list.length > 0 && list.length < total" class="loadmore">上拉加载更多</view>
  </view>
</template>

<style scoped>
.page {
  padding: 24rpx 32rpx;
}
.search {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.search__input {
  flex: 1;
  padding: 18rpx 20rpx;
  background-color: #ffffff;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
}
.search__btn {
  width: 140rpx;
  background-color: #2563eb;
  color: #ffffff;
  font-size: 28rpx;
  border-radius: 12rpx;
}
.card {
  padding: 28rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}
.card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.card__title {
  font-size: 32rpx;
  font-weight: 600;
}
.card__tag {
  font-size: 22rpx;
  color: #2563eb;
  background-color: #eff6ff;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
}
.card__line {
  display: block;
  font-size: 26rpx;
  color: #6b7280;
  margin-top: 6rpx;
}
.empty {
  text-align: center;
  color: #9ca3af;
  padding: 80rpx 0;
}
.loadmore {
  text-align: center;
  color: #9ca3af;
  font-size: 24rpx;
  padding: 20rpx 0;
}
</style>
