import { createApp } from 'vue'
// element-plus 样式按需引入（组件在页面内显式 import，保证 vue-tsc 类型与树摇）
import 'element-plus/dist/index.css'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'

const app = createApp(App)

app.use(pinia)
app.use(router)

app.mount('#app')