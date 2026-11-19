import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { useAuthStore } from '@/stores/auth'
import { hasPermission } from '@/composables/usePermission'

// Electron（file:// 协议）下 pushState 受限，改用 hash 历史模式；Web 部署保持 history 模式
const history =
  import.meta.env.VITE_ROUTER_MODE === 'hash'
    ? createWebHashHistory(import.meta.env.BASE_URL)
    : createWebHistory(import.meta.env.BASE_URL)

export const router = createRouter({
  history,
  routes
})

/** 无需鉴权的公开路径（登录页白名单；含企业微信扫码登录回调页）。 */
const PUBLIC_PATHS = ['/login', '/login/wecom']

/** 菜单顺序与权限码：无权限时路由守卫回退到第一个可访问页面。 */
const MENU_ACCESS: Array<[path: string, permission: string]> = [
  ['/todo', 'bpm'],
  ['/masterdata', 'masterdata'],
  ['/project', 'project'],
  ['/hr', 'hr'],
  ['/admin', 'admin'],
  ['/doc', 'doc'],
  ['/integration/wecom', 'integration:wecom:view'],
  ['/expense', 'expense'],
  ['/payment', 'payment'],
  ['/bank', 'bank'],
  ['/seal', 'seal'],
  ['/notification', 'notification'],
  ['/report', 'report'],
  ['/account', 'iam']
]

/** 当前账号可访问的第一个菜单；一个都没有时回退到登录页，避免死循环。 */
function firstAllowedPath(): string {
  const found = MENU_ACCESS.find(([, permission]) => hasPermission(permission))
  return found ? found[0] : '/login'
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!PUBLIC_PATHS.includes(to.path) && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (auth.isAuthenticated) {
    // 登录后按后端权限码渲染菜单；角色权限调整后刷新页面即可生效
    await auth.loadPermissions()
    const required = to.meta.permission
    if (required && !hasPermission(required)) {
      const fallback = firstAllowedPath()
      if (fallback !== to.path) {
        return { path: fallback, replace: true }
      }
    }
  }

  const title = typeof to.meta.title === 'string' ? to.meta.title : 'CEAMS'
  document.title = `${title} - CEAMS`
  return true
})
