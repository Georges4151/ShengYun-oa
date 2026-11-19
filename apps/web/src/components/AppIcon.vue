<script setup lang="ts">
import { computed } from 'vue'

/**
 * 内联 SVG 线性图标组件。
 *
 * 说明：
 * - 统一 24x24 viewBox、1.8 描边、圆角端点，视觉风格与设计令牌保持一致；
 * - 描边色默认 currentColor，Web 端可直接继承父级文字色；
 * - ICONS 为本地静态常量（不含任何用户输入），因此可安全内联渲染。
 */
defineOptions({ name: 'AppIcon' })

const props = withDefaults(
  defineProps<{
    /** 图标名称（见 ICONS 键名，未匹配时回退到圆点） */
    name: string
    /** 图标边长（px） */
    size?: number
    /** 描边颜色，默认继承当前文字色 */
    color?: string
  }>(),
  { size: 18, color: 'currentColor' }
)

/** 线性图标片段库：键名与菜单 / 快捷入口的 icon 字段一一对应。 */
const ICONS: Record<string, string> = {
  // 仪表盘 / 四宫格
  dashboard:
    '<rect x="3.5" y="3.5" width="7.5" height="7.5" rx="1.6"/><rect x="13" y="3.5" width="7.5" height="7.5" rx="1.6"/><rect x="3.5" y="13" width="7.5" height="7.5" rx="1.6"/><rect x="13" y="13" width="7.5" height="7.5" rx="1.6"/>',
  // 清单 / 待办
  todo: '<rect x="5" y="4.5" width="14" height="16.5" rx="2"/><path d="M9 4.5V3h6v1.5"/><path d="M8.5 9.5h7"/><path d="M8.5 13h7"/><path d="M8.5 16.5h4"/>',
  // 数据库 / 主数据
  masterdata:
    '<ellipse cx="12" cy="6" rx="7" ry="2.8"/><path d="M5 6v12c0 1.55 3.13 2.8 7 2.8s7-1.25 7-2.8V6"/><path d="M5 12c0 1.55 3.13 2.8 7 2.8s7-1.25 7-2.8"/>',
  // 楼宇 / 项目
  project:
    '<path d="M12 3 4.5 6.2V21"/><path d="M12 3l7.5 3.2V21"/><path d="M2.5 21h19"/><path d="M9.5 21v-5h5v5"/><path d="M8 9h1.5M14.5 9H16M8 12.5h1.5M14.5 12.5H16"/>',
  // 人员 / 人事
  hr: '<circle cx="9" cy="8" r="3.2"/><path d="M3.5 20c0-3.03 2.46-5.5 5.5-5.5s5.5 2.47 5.5 5.5"/><path d="M16 5.6a3 3 0 0 1 0 5.4"/><path d="M17.7 14.7c1.7.67 2.8 2.35 2.8 4.3"/>',
  // 盒子 / 行政后勤
  admin: '<path d="M12 3 4 7v10l8 4 8-4V7l-8-4Z"/><path d="M4 7l8 4 8-4"/><path d="M12 11v10"/>',
  // 文档
  doc: '<path d="M14 3H7.5A1.5 1.5 0 0 0 6 4.5v15A1.5 1.5 0 0 0 7.5 21h9a1.5 1.5 0 0 0 1.5-1.5V7l-4-4Z"/><path d="M13.5 3v4.5H18"/><path d="M9 12.5h6M9 16h4"/>',
  // 消息气泡 / 企业微信
  wecom:
    '<path d="M20.5 11.8c0 3.7-3.8 6.7-8.5 6.7-1.03 0-2.02-.14-2.93-.4L4.5 20l1.35-3.4C4.42 15.28 3.5 13.63 3.5 11.8 3.5 8.1 7.3 5.1 12 5.1s8.5 3 8.5 6.7Z"/><path d="M9 11.8h.01M12 11.8h.01M15 11.8h.01"/>',
  // 票据 / 报销
  expense:
    '<path d="M6.5 3h11v18l-2.75-1.8L12 21l-2.75-1.8L6.5 21V3Z"/><path d="M9.5 8h5M9.5 12h5"/>',
  // 银行卡 / 资金
  payment: '<rect x="2.5" y="5.5" width="19" height="13" rx="2"/><path d="M2.5 10h19"/><path d="M6 14.5h4"/>',
  // 双向箭头 / 对账
  bank: '<path d="M4 8h13"/><path d="M13.5 4.5 17 8l-3.5 3.5"/><path d="M20 16H7"/><path d="M10.5 12.5 7 16l3.5 3.5"/>',
  // 印章
  seal: '<circle cx="12" cy="8.5" r="5"/><path d="M9.8 13.4 8.5 18.5h7l-1.3-5.1"/><path d="M5.5 21h13"/>',
  // 铃铛 / 通知
  notification:
    '<path d="M18 16.5H6l1.4-2.1V10a4.6 4.6 0 0 1 9.2 0v4.4l1.4 2.1Z"/><path d="M10 19.5a2.2 2.2 0 0 0 4 0"/>',
  // 柱状图 / 报表
  report:
    '<path d="M4 20.5h16"/><rect x="6" y="11.5" width="3" height="6.5" rx="1"/><rect x="10.5" y="7" width="3" height="11" rx="1"/><rect x="15" y="13.5" width="3" height="4.5" rx="1"/>',
  // 齿轮 / 账号
  account:
    '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1Z"/>',
  // 时钟 / 超时
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 1.8"/>',
  // 收件箱 / 我发起的
  inbox:
    '<path d="M20.5 13.5v5a1.5 1.5 0 0 1-1.5 1.5H5a1.5 1.5 0 0 1-1.5-1.5v-5"/><path d="M3.5 13.5 6 4.5h12l2.5 9"/><path d="M8 13.5h2.2l1 2.2h1.6l1-2.2H16"/>'
}

/** 未匹配时回退为圆点，保证布局与尺寸稳定。 */
const fallback = '<circle cx="12" cy="12" r="4"/>'

const markup = computed(() => ICONS[props.name] ?? fallback)
</script>

<template>
  <svg
    class="app-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    :stroke="color"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
    v-html="markup"
  />
</template>

<style scoped>
.app-icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
