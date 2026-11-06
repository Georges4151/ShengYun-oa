<script setup lang="ts">
import { computed, type CSSProperties } from 'vue'
import { formatMoney } from '@ceams/utils'

type AmountSize = 'lg' | 'md' | 'sm'

const props = withDefaults(
  defineProps<{
    /** 金额：数字或后端返回的金额字符串 */
    value: number | string
    /** 展示尺寸 */
    size?: AmountSize
    /** 正数是否显示 + 号 */
    signed?: boolean
    /** 自定义颜色，可传颜色值或 CSS 变量（如 var(--ceams-color-danger)） */
    color?: string
  }>(),
  { size: 'md', signed: false, color: '' }
)

const displayValue = computed<string>(() => {
  const formatted = formatMoney(props.value)
  if (!props.signed || formatted === '--') return formatted
  return Number(props.value) > 0 ? `+${formatted}` : formatted
})

const colorStyle = computed<CSSProperties>(() => (props.color ? { color: props.color } : {}))
</script>

<template>
  <text class="ceams-amount-text" :class="'ceams-amount-text--' + props.size" :style="colorStyle">
    {{ displayValue }}
  </text>
</template>

<style scoped>
.ceams-amount-text {
  display: inline-block;
  color: var(--ceams-text-amount);
  font-weight: var(--ceams-font-weight-semibold);
  letter-spacing: var(--ceams-letter-spacing-number);
  line-height: var(--ceams-line-height-tight);
}

.ceams-amount-text--lg {
  font-size: var(--ceams-font-size-amount-lg);
  font-weight: var(--ceams-font-weight-bold);
}

.ceams-amount-text--md {
  font-size: var(--ceams-font-size-amount-md);
}

.ceams-amount-text--sm {
  font-size: var(--ceams-font-size-amount-sm);
  font-weight: var(--ceams-font-weight-medium);
}
</style>
