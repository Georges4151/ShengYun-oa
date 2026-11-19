import { createApp } from 'vue'
// element-plus 样式按需引入（组件在页面内显式 import，保证 vue-tsc 类型与树摇）
import 'element-plus/dist/index.css'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'
import { initDesktopDeepLink } from './services/desktop'

const app = createApp(App)

app.use(pinia)
app.use(router)

// 桌面端：主进程把 ceams:// 深链转成应用内路由（Web 环境为空操作）
initDesktopDeepLink(router)

app.mount('#app')