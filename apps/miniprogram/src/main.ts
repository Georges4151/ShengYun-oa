import { createSSRApp } from 'vue'
import App from './App.vue'

// uni-app Vue3 标准入口：返回 { app } 供各平台渲染。
export function createApp() {
  const app = createSSRApp(App)
  return { app }
}