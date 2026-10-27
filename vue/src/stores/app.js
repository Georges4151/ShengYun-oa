import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    device: 'desktop',
    loading: false,
    breadcrumbList: []
  }),

  getters: {
    isMobile: (state) => state.device === 'mobile'
  },

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },

    setDevice(device) {
      this.device = device
    },

    setLoading(loading) {
      this.loading = loading
    },

    setBreadcrumb(list) {
      this.breadcrumbList = list
    }
  }
})
