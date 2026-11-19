<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    /** 空态标题 */
    title: string
    /** 补充说明，可选 */
    description?: string
    /** 已废弃：图标统一使用 app-icon 的 empty 图标，保留字段兼容旧调用 */
    icon?: string
  }>(),
  { description: '', icon: '' }
)
</script>

<template>
  <view class="ceams-empty-state">
    <app-icon class="ceams-empty-state__icon" name="empty" :size="64" color="#9ca3af" />
    <text class="ceams-empty-state__title">{{ props.title }}</text>
    <text v-if="props.description" class="ceams-empty-state__description">
      {{ props.description }}
    </text>
    <view v-if="$slots.default" class="ceams-empty-state__action">
      <slot />
    </view>
  </view>
</template>

<style scoped>
.ceams-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  /* 上下各 48rpx 留白，保证卡片内空态有呼吸感 */
  padding: 48rpx var(--ceams-space-lg);
  text-align: center;
}

.ceams-empty-state__icon {
  margin-bottom: 16rpx;
}

.ceams-empty-state__title {
  color: var(--ceams-text-secondary);
  font-size: 26rpx;
  font-weight: var(--ceams-font-weight-medium);
  line-height: var(--ceams-line-height-normal);
}

.ceams-empty-state__description {
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-placeholder);
  font-size: 22rpx;
  line-height: var(--ceams-line-height-normal);
}

.ceams-empty-state__action {
  margin-top: var(--ceams-space-lg);
}
</style>
