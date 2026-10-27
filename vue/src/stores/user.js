import { defineStore } from 'pinia'
import { login, logout, getUserInfo } from '@/api/auth'
import { getToken, setToken, removeToken, setUserInfo, removeUserInfo, clearAuth } from '@/utils/auth'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: null,
    roles: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo?.username || '',
    userId: (state) => state.userInfo?.id || null,
    role: (state) => state.userInfo?.role || ''
  },

  actions: {
    async login(loginForm) {
      try {
        const res = await login(loginForm)
        this.token = res.token
        setToken(res.token)
        
        if (res.user) {
          this.userInfo = res.user
          setUserInfo(res.user)
        }
        
        return res
      } catch (error) {
        return Promise.reject(error)
      }
    },

    async fetchUserInfo() {
      try {
        const res = await getUserInfo()
        this.userInfo = res
        setUserInfo(res)
        return res
      } catch (error) {
        return Promise.reject(error)
      }
    },

    async logout() {
      try {
        await logout()
      } catch (error) {
        console.error('Logout error:', error)
      } finally {
        this.resetState()
        router.push('/login')
      }
    },

    resetState() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      clearAuth()
    }
  }
})
