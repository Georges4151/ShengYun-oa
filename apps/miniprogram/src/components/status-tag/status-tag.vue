<script setup lang="ts">
/** 状态语义枚举：对应审批、核验、付款等业务状态 */
type StatusTagType = 'pending' | 'approved' | 'rejected' | 'verified' | 'paid' | 'neutral'

const props = withDefaults(
  defineProps<{
    /** 徽标文案 */
    label: string
    /** 语义类型，决定徽标配色 */
    type?: StatusTagType
  }>(),
  { type: 'neutral' }
)
</script>

<template>
  <view class="ceams-status-tag" :class="'ceams-status-tag--' + props.type">
    <text>{{ props.label }}</text>
  </view>
</template>

<style scoped>
.ceams-status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--ceams-space-2xs) var(--ceams-space-sm);
  border-radius: var(--ceams-radius-tag);
  font-size: var(--ceams-font-size-mini);
  font-weight: var(--ceams-font-weight-medium);
  line-height: var(--ceams-line-height-tight);
}

.ceams-status-tag--pending {
  color: var(--ceams-status-pending-text);
  background-color: var(--ceams-status-pending-bg);
}

.ceams-status-tag--approved {
  color: var(--ceams-status-approved-text);
  background-color: var(--ceams-status-approved-bg);
}

.ceams-status-tag--rejected {
  color: var(--ceams-status-rejected-text);
  background-color: var(--ceams-status-rejected-bg);
}

.ceams-status-tag--verified {
  color: var(--ceams-status-verified-text);
  background-color: var(--ceams-status-verified-bg);
}

.ceams-status-tag--paid {
  color: var(--ceams-status-paid-text);
  background-color: var(--ceams-status-paid-bg);
}

.ceams-status-tag--neutral {
  color: var(--ceams-status-neutral-text);
  background-color: var(--ceams-status-neutral-bg);
}
</style>
