<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    /** 空态标题 */
    title: string
    /** 补充说明，可选 */
    description?: string
    /** 文本/字符图标，可选；不引入图标库 */
    icon?: string
  }>(),
  { description: '', icon: '' }
)
</script>

<template>
  <view class="ceams-empty-state">
    <view v-if="props.icon" class="ceams-empty-state__icon">{{ props.icon }}</view>
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
  padding: var(--ceams-space-3xl) var(--ceams-space-lg);
  text-align: center;
}

.ceams-empty-state__icon {
  margin-bottom: var(--ceams-space-sm);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-space-3xl);
  line-height: var(--ceams-line-height-tight);
}

.ceams-empty-state__title {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-medium);
  line-height: var(--ceams-line-height-normal);
}

.ceams-empty-state__description {
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.ceams-empty-state__action {
  margin-top: var(--ceams-space-lg);
}
</style>
