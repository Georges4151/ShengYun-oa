<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    /** 主标题，可选（也可通过 left 插槽自定义） */
    title?: string
    /** 副标题，可选 */
    subtitle?: string
    /** 右侧辅助信息，可选（也可通过 right 插槽自定义） */
    meta?: string
  }>(),
  { title: '', subtitle: '', meta: '' }
)
</script>

<template>
  <view class="ceams-list-card">
    <view class="ceams-list-card__head">
      <view class="ceams-list-card__main">
        <slot name="left">
          <text v-if="props.title" class="ceams-list-card__title">{{ props.title }}</text>
          <text v-if="props.subtitle" class="ceams-list-card__subtitle">{{ props.subtitle }}</text>
        </slot>
      </view>
      <view class="ceams-list-card__extra">
        <slot name="right">
          <text v-if="props.meta" class="ceams-list-card__meta">{{ props.meta }}</text>
        </slot>
      </view>
    </view>
    <view v-if="$slots.default" class="ceams-list-card__body">
      <slot />
    </view>
  </view>
</template>

<style scoped>
.ceams-list-card {
  margin-bottom: var(--ceams-space-md);
  padding: 24rpx;
  background-color: var(--ceams-bg-card);
  border-radius: 16rpx;
  /* 极轻阴影：与首页工作台卡片观感一致，避免列表页视觉过重 */
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.04);
  box-sizing: border-box;
}

.ceams-list-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.ceams-list-card__main {
  flex: 1;
  min-width: 0;
}

.ceams-list-card__title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: 28rpx;
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.ceams-list-card__subtitle {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
  line-height: var(--ceams-line-height-normal);
}

.ceams-list-card__extra {
  flex-shrink: 0;
  margin-left: var(--ceams-space-md);
}

.ceams-list-card__meta {
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
  line-height: var(--ceams-line-height-normal);
}

.ceams-list-card__body {
  margin-top: var(--ceams-space-sm);
}
</style>
