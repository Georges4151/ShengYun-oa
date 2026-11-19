import { createApp } from 'vue'
import { ElLoading } from 'element-plus'
// element-plus 样式按需引入（组件在页面内显式 import，保证 vue-tsc 类型与树摇）
import 'element-plus/dist/index.css'
// 设计令牌与全局视觉基线（必须位于 Element Plus 默认样式之后，以便覆盖主题变量）
import './styles/tokens.css'
import './styles/global.css'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'
import { initDesktopDeepLink } from './services/desktop'

const app = createApp(App)

app.use(pinia)
app.use(router)
// 注册 ElLoading 插件：全局提供 v-loading 指令与 $loading 服务（本项目 Element Plus 按需引入，未整体 app.use(ElementPlus)）
app.use(ElLoading)

// 桌面端：主进程把 ceams:// 深链转成应用内路由（Web 环境为空操作）
initDesktopDeepLink(router)

app.mount('#app')