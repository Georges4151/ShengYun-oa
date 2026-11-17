import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { useAuthStore } from '@/stores/auth'

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

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!PUBLIC_PATHS.includes(to.path) && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const title = typeof to.meta.title === 'string' ? to.meta.title : 'CEAMS'
  document.title = `${title} - CEAMS`
  return true
})
