import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { useAuthStore } from '@/stores/auth'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

/** 无需鉴权的公开路径（登录页白名单）。 */
const PUBLIC_PATHS = ['/login']

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!PUBLIC_PATHS.includes(to.path) && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const title = typeof to.meta.title === 'string' ? to.meta.title : 'CEAMS'
  document.title = `${title} - CEAMS`
  return true
})
