import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { LoginData, LoginRequest, UserVO, WecomLoginBody } from '@ceams/types'
import { ceamsApi } from '@/services/http'

const ACCESS_TOKEN_KEY = 'ceams.access_token'
const REFRESH_TOKEN_KEY = 'ceams.refresh_token'
const USER_KEY = 'ceams.user'

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

/**
 * 认证状态：access / refresh 令牌 + 当前用户。
 * token 为 access token；refreshToken 用于令牌轮换。
 * token / refreshToken / user 持久化到 localStorage，刷新页面后仍可恢复会话。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(readStored(ACCESS_TOKEN_KEY))
  const refreshToken = ref<string | null>(readStored(REFRESH_TOKEN_KEY))
  const user = ref<UserVO | null>(readStoredUser())

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
    persist()
  }

  /** 登录：调用 iam.login 后保存令牌与用户信息。 */
  async function login(body: LoginRequest): Promise<void> {
    const data = await ceamsApi.iam.login(body)
    setSession(data)
  }

  /**
   * 企业微信扫码登录：用回调页拿到的 code/state 换取令牌对，
   * 之后复用 setSession 落库（与账号密码登录共享同一套会话存储逻辑）。
   */
  async function loginByWecom(body: WecomLoginBody): Promise<void> {
    const data = await ceamsApi.integration.loginByWecom(body)
    setSession(data)
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
    isAuthenticated,
    username,
    name,
    roles,
    setSession,
    setUser,
    clear,
    login,
    loginByWecom,
    logout
  }
})
