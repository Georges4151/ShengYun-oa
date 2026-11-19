<script setup lang="ts">
/**
 * CEAMS 统一图标组件
 * 实现方式：内联 SVG 字符串 → data:image/svg+xml URI → <image> 渲染。
 * 不引入任何图标库，不使用 v-html（小程序不支持）。
 * 小程序 <image> 不识别 currentColor，因此颜色在生成 URI 时写入 SVG。
 */
import { computed } from 'vue'

/** 默认图标颜色（品牌主色） */
const DEFAULT_COLOR = '#2563eb'

/** 默认尺寸（rpx） */
const DEFAULT_SIZE = 40

/** 颜色占位符：仅用于需要实心填充的局部图形 */
const COLOR_SLOT = '__CEAMS_ICON_COLOR__'

/** 通用回退图标：圆点（name 未匹配时使用，保证不空白、不报错） */
const FALLBACK_ICON = `<circle cx="12" cy="12" r="5" /><circle cx="12" cy="12" r="1.5" fill="${COLOR_SLOT}" stroke="none" />`

/**
 * 图标路径定义（24x24 画布，线性风格）
 * 统一约定：stroke-width 1.8、round 端点与连接、fill 由外层统一关闭。
 */
const ICON_PATHS = {
  /* 项目：楼宇轮廓，带窗户 */
  project: `<path d="M4.2 20.5V6.4a1.2 1.2 0 0 1 1.2-1.2h6.4a1.2 1.2 0 0 1 1.2 1.2v14.1" />
    <path d="M13 10.8h5.6a1.2 1.2 0 0 1 1.2 1.2v8.5" />
    <path d="M3 20.5h18" />
    <path d="M7 8.6h2.2M7 12h2.2M7 15.4h2.2M16 14h1.4M16 17.2h1.4" />`,
  /* 待办：清单板 + 勾选 */
  todo: `<rect x="5" y="4" width="14" height="16.6" rx="2.4" />
    <path d="M9 2.9h6v2.2H9z" />
    <path d="M8.6 13.2l2.3 2.3 4.5-4.6" />`,
  /* 报销：票据金额（¥ 硬币） */
  expense: `<circle cx="12" cy="12" r="8.4" />
    <path d="M9 8.4l3 3.7 3-3.7" />
    <path d="M12 12.1v5.2" />
    <path d="M9.4 13.4h5.2M9.4 15.4h5.2" />`,
  /* 打卡：时钟 */
  checkin: `<circle cx="12" cy="12" r="8.4" />
    <path d="M12 7.4V12l3.2 1.9" />`,
  /* 请假：日历 + 勾选 */
  leave: `<rect x="4" y="5.4" width="16" height="15.2" rx="2.4" />
    <path d="M8.2 3.2v4.2M15.8 3.2v4.2" />
    <path d="M4 10.2h16" />
    <path d="M9.4 14.6l1.9 1.9 3.5-3.6" />`,
  /* 我的：单人头像 */
  profile: `<circle cx="12" cy="8.2" r="3.9" />
    <path d="M4.8 20.4c0-3.7 3.2-6.2 7.2-6.2s7.2 2.5 7.2 6.2" />`,
  /* 薪资：钱袋 */
  salary: `<path d="M9.2 3.4h5.6l-1.4 3.2h-2.8L9.2 3.4z" />
    <path d="M10.6 6.6C8 8 6.2 10.5 6.2 13.6c0 3.9 2.6 6.9 5.8 6.9s5.8-3 5.8-6.9c0-3.1-1.8-5.6-4.4-7" />
    <path d="M9.4 14.4c1.5-1.2 3.7-1.2 5.2 0" />`,
  /* 绩效：五角星 */
  performance: `<path d="M12 3.6l2.55 5.2 5.75.85-4.15 4.05 1 5.7L12 16.7l-5.15 2.7 1-5.7L3.7 9.65l5.75-.85L12 3.6z" />`,
  /* 变动：双向箭头 */
  change: `<path d="M4.4 8.8h14.2" />
    <path d="M15.4 5.6l3.2 3.2-3.2 3.2" />
    <path d="M19.6 15.2H5.4" />
    <path d="M8.6 12l-3.2 3.2 3.2 3.2" />`,
  /* 付款：银行卡 */
  payment: `<rect x="3" y="5.6" width="18" height="12.8" rx="2.4" />
    <path d="M3 10.2h18" />
    <path d="M6.6 14.8h3.6" />`,
  /* 通知：铃铛 */
  notification: `<path d="M12 3.4a5.6 5.6 0 0 0-5.6 5.6v3.1L4.8 15.5h14.4l-1.6-3.4V9A5.6 5.6 0 0 0 12 3.4z" />
    <path d="M9.9 18.3a2.3 2.3 0 0 0 4.2 0" />`,
  /* 文档：折角文件 */
  doc: `<path d="M6.4 3.4h7l4.2 4.2v13H6.4z" />
    <path d="M13.4 3.4v4.2h4.2" />
    <path d="M9.4 12.2h5.2M9.4 15.6h5.2" />`,
  /* AI：星芒 */
  ai: `<path d="M11 3.4l1.75 4.75L17.5 9.9l-4.75 1.75L11 16.4l-1.75-4.75L4.5 9.9l4.75-1.75L11 3.4z" />
    <path d="M17.6 15.4l.85 2.15 2.15.85-2.15.85-.85 2.15-.85-2.15-2.15-.85 2.15-.85.85-2.15z" />`,
  /* 维修：扳手 */
  repair: `<path d="M20.2 5.1a5.1 5.1 0 0 1-6.6 6.5l-6.3 6.3a2.1 2.1 0 1 1-3-3l6.3-6.3a5.1 5.1 0 0 1 6.5-6.6l-2.6 2.6.4 2.7 2.7.4 2.6-2.6z" />`,
  /* 资产：立体盒子 */
  asset: `<path d="M3.4 8.6L12 4.2l8.6 4.4L12 13z" />
    <path d="M3.4 8.6v7.2L12 20.2l8.6-4.4V8.6" />
    <path d="M12 13v7.2" />`,
  /* 车辆：小车 */
  vehicle: `<path d="M4.4 16.6v-3.4l2-4.5a2 2 0 0 1 1.8-1.2h7.6a2 2 0 0 1 1.8 1.2l2 4.5v3.4" />
    <path d="M3.4 13.2h17.2" />
    <circle cx="7.6" cy="17.4" r="1.7" />
    <circle cx="16.4" cy="17.4" r="1.7" />
    <path d="M9.3 17.4h5.4" />`,
  /* 会议：显示屏幕 + 支架 */
  meeting: `<rect x="3.2" y="4.6" width="17.6" height="11" rx="2.2" />
    <path d="M12 15.6v3.2M8.4 18.8h7.2" />
    <path d="M8.6 10.1h.01M12 10.1h.01M15.4 10.1h.01" />`,
  /* 访客：工牌 */
  visitor: `<rect x="6.2" y="4.2" width="11.6" height="16.4" rx="2" />
    <path d="M10.2 4.2V2.9h3.6v1.3" />
    <circle cx="12" cy="10.6" r="1.9" />
    <path d="M8.9 16.2c0-1.7 1.4-2.8 3.1-2.8s3.1 1.1 3.1 2.8" />`,
  /* 首页：房子 */
  home: `<path d="M4.4 10.7L12 4.2l7.6 6.5V19a1.4 1.4 0 0 1-1.4 1.4h-3.4v-5.6h-4.8v5.6H5.8A1.4 1.4 0 0 1 4.4 19v-8.3z" />`,
  /* 右箭头 */
  'arrow-right': `<path d="M4.6 12h14.2" />
    <path d="M13.2 6.4l5.6 5.6-5.6 5.6" />`,
  /* 加号 */
  plus: `<path d="M12 5.2v13.6M5.2 12h13.6" />`,
  /* 搜索：放大镜 */
  search: `<circle cx="10.8" cy="10.8" r="6.2" />
    <path d="M15.4 15.4l4.6 4.6" />`,
  /* 筛选：漏斗 */
  filter: `<path d="M3.8 5.4h16.4l-6.3 7.5v5.4l-3.8-1.9v-3.5L3.8 5.4z" />`,
  /* 刷新：环形箭头 */
  refresh: `<path d="M19.6 12a7.6 7.6 0 1 1-2.4-5.5" />
    <path d="M20 3.9v4.3h-4.3" />`,
  /* 空态：敞口盒子 */
  empty: `<path d="M4 13v5.4a1.6 1.6 0 0 0 1.6 1.6h12.8a1.6 1.6 0 0 0 1.6-1.6V13" />
    <path d="M4 13l2.2-6.4a1.6 1.6 0 0 1 1.5-1.1h8.6a1.6 1.6 0 0 1 1.5 1.1L20 13" />
    <path d="M4 13h4.6l1 2.1h4.8l1-2.1H20" />`,
  /* 警告：三角感叹号 */
  warning: `<path d="M12 3.8l8.7 15.4H3.3L12 3.8z" />
    <path d="M12 9.4v4.3" />
    <path d="M12 16.7h.01" />`
} as const

/** 已收录的图标名 */
type IconName = keyof typeof ICON_PATHS

const props = withDefaults(
  defineProps<{
    /** 图标名，见组件内 ICON_PATHS；未匹配时回退为通用圆点 */
    name: string
    /** 尺寸：数字按 rpx 处理，字符串原样使用（如 '32px'） */
    size?: number | string
    /** 图标颜色（写入 SVG 的 stroke / 局部 fill） */
    color?: string
  }>(),
  {
    size: DEFAULT_SIZE,
    color: DEFAULT_COLOR
  }
)

/** 数值尺寸转为 rpx；带单位的字符串保持原样 */
const sizeRpx = computed(() => {
  const raw = props.size
  if (typeof raw === 'number') {
    return `${raw}rpx`
  }
  const text = String(raw).trim()
  if (!text) {
    return `${DEFAULT_SIZE}rpx`
  }
  return /^\d+(\.\d+)?$/.test(text) ? `${text}rpx` : text
})

const iconStyle = computed(() => ({
  width: sizeRpx.value,
  height: sizeRpx.value
}))

/** 转义颜色值，避免破坏 SVG 属性 */
function escapeColor(value: string): string {
  return value.replace(/[&<>"']/g, (char) => {
    switch (char) {
      case '&':
        return '&amp;'
      case '<':
        return '&lt;'
      case '>':
        return '&gt;'
      case '"':
        return '&quot;'
      default:
        return '&#39;'
    }
  })
}

/** 生成 data URI（SVG 内容整体 encodeURIComponent，确保 # 等字符安全） */
const iconUri = computed(() => {
  const body = ICON_PATHS[props.name as IconName] ?? FALLBACK_ICON
  const color = escapeColor(props.color || DEFAULT_COLOR)
  const svg =
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none"' +
    ` stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">` +
    body.split(COLOR_SLOT).join(color) +
    '</svg>'
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
})
</script>

<template>
  <image class="ceams-app-icon" :src="iconUri" :style="iconStyle" mode="aspectFit" />
</template>

<style scoped>
.ceams-app-icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
