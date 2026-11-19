import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { LoginData, LoginRequest, UserVO, WecomLoginBody } from '@ceams/types'
import { ceamsApi } from '@/services/http'

const ACCESS_TOKEN_KEY = 'ceams.access_token'
const REFRESH_TOKEN_KEY = 'ceams.refresh_token'
const USER_KEY = 'ceams.user'
const PERMISSIONS_KEY = 'ceams.permissions'

function readStored(key: string): string | null {
  try {
    return window.localStorage.getItem(key)
  } catch {
    return null
  }
}

function readStoredUser(): UserVO | null {
  const raw = readStored(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserVO
  } catch {
    return null
  }
}

/** 读取缓存的权限码（刷新页面时先按缓存渲染菜单，避免闪烁）。 */
function readStoredPermissions(): string[] {
  const raw = readStored(PERMISSIONS_KEY)
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as unknown
    return Array.isArray(parsed) ? parsed.filter((code): code is string => typeof code === 'string') : []
  } catch {
    return []
  }
}

/**
 * 认证状态：access / refresh 令牌 + 当前用户。
 * token 为 access token；refreshToken 用于令牌轮换。
 * token / refreshToken / user 持久化到 localStorage，刷新页面后仍可恢复会话。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(readStored(ACCESS_TOKEN_KEY))
  const refreshToken = ref<string | null>(readStored(REFRESH_TOKEN_KEY))
  const user = ref<UserVO | null>(readStoredUser())
  /** 当前账号后端下发的权限码（角色权限变更后需重新登录或刷新页面生效）。 */
  const permissions = ref<string[]>(readStoredPermissions())
  /** 是否已从后端拉取过权限码；未拉取时按钮判定回退到角色映射。 */
  const permissionsLoaded = ref(permissions.value.length > 0)

  const isAuthenticated = computed(() => token.value !== null)
  /** 登录账号名。 */
  const username = computed(() => user.value?.username ?? '')
  /** 展示姓名。 */
  const name = computed(() => user.value?.name ?? '')
  /** 当前用户角色集合。 */
  const roles = computed(() => user.value?.roles ?? [])

  function persist(): void {
    try {
      if (token.value) window.localStorage.setItem(ACCESS_TOKEN_KEY, token.value)
      else window.localStorage.removeItem(ACCESS_TOKEN_KEY)
      if (refreshToken.value) window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken.value)
      else window.localStorage.removeItem(REFRESH_TOKEN_KEY)
      if (user.value) window.localStorage.setItem(USER_KEY, JSON.stringify(user.value))
      else window.localStorage.removeItem(USER_KEY)
      if (permissions.value.length > 0) {
        window.localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(permissions.value))
      } else {
        window.localStorage.removeItem(PERMISSIONS_KEY)
      }
    } catch {
      // localStorage 不可用时忽略（如隐私模式），会话仍可在内存中工作
    }
  }

  /** 保存登录 / 刷新返回的完整会话（令牌对 + 用户）。 */
  function setSession(data: LoginData): void {
    token.value = data.tokenPair.accessToken
    refreshToken.value = data.tokenPair.refreshToken
    user.value = data.user
    persist()
  }

  function setUser(value: UserVO | null): void {
    user.value = value
    persist()
  }

  /** 清空本地会话状态。 */
  function clear(): void {
    token.value = null
    refreshToken.value = null
    user.value = null
    permissions.value = []
    permissionsLoaded.value = false
    persist()
  }

  /**
   * 从后端拉取当前账号权限码（用于菜单/按钮级控制）。
   * 拉取失败时保留缓存值，避免网络抖动导致菜单全部消失。
   */
  async function loadPermissions(force = false): Promise<void> {
    if (!token.value) return
    if (permissionsLoaded.value && !force) return
    try {
      permissions.value = await ceamsApi.iam.permissionMine()
      permissionsLoaded.value = true
      persist()
    } catch {
      // 保留已有缓存；后续路由守卫会再次尝试
    }
  }

  /** 登录：调用 iam.login 后保存令牌与用户信息。 */
  async function login(body: LoginRequest): Promise<void> {
    const data = await ceamsApi.iam.login(body)
    setSession(data)
    await loadPermissions(true)
  }

  /**
   * 企业微信扫码登录：用回调页拿到的 code/state 换取令牌对，
   * 之后复用 setSession 落库（与账号密码登录共享同一套会话存储逻辑）。
   */
  async function loginByWecom(body: WecomLoginBody): Promise<void> {
    const data = await ceamsApi.integration.loginByWecom(body)
    setSession(data)
    await loadPermissions(true)
  }

  /** 登出：调用后端注销接口并清空本地会话。 */
  async function logout(): Promise<void> {
    try {
      await ceamsApi.iam.logout()
    } finally {
      clear()
    }
  }

  return {
    token,
    refreshToken,
    user,
    permissions,
    permissionsLoaded,
    isAuthenticated,
    username,
    name,
    roles,
    setSession,
    setUser,
    clear,
    loadPermissions,
    login,
    loginByWecom,
    logout
  }
})
