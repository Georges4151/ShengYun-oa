import { createSSRApp } from 'vue'
import App from './App.vue'
import StatusTag from './components/status-tag/status-tag.vue'
import EmptyState from './components/empty-state/empty-state.vue'
import ListCard from './components/list-card/list-card.vue'
import AmountText from './components/amount-text/amount-text.vue'

// uni-app Vue3 标准入口：返回 { app } 供各平台渲染。
export function createApp() {
  const app = createSSRApp(App)
  // 设计基础层组件全局注册：小程序端会写入 app.json 的 usingComponents，
  // 页面无需 import 即可直接使用；组件目录仍遵循 easycom 默认规范。
  app.component('status-tag', StatusTag)
  app.component('empty-state', EmptyState)
  app.component('list-card', ListCard)
  app.component('amount-text', AmountText)
  return { app }
}