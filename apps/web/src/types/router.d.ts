import 'vue-router'

/**
 * vue-router 路由元信息扩展。
 * 必须以「模块增强」方式声明（先 import 再 declare module），
 * 避免在全局脚本文件中被误解析为 ambient module 声明而覆盖真实导出。
 */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    /** 页面访问所需的权限码（与 V39 等权限种子一致，供守卫/菜单按需判断） */
    permission?: string
    /** 菜单图标标识（Element Plus 图标名，当前菜单未渲染图标，仅作约定保留） */
    icon?: string
  }
}
