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
  }
}
