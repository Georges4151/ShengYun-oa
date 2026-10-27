import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 认证状态占位：token / 当前用户。
 * 真实登录流程待后端 iam 契约冻结后接入。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const username = ref('')

  const isAuthenticated = computed(() => token.value !== null)

  function setToken(value: string | null): void {
    token.value = value
  }

  function setUsername(value: string): void {
    username.value = value
  }

  function logout(): void {
    token.value = null
    username.value = ''
  }

  return { token, username, isAuthenticated, setToken, setUsername, logout }
})