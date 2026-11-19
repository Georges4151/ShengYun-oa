import { createPinia } from 'pinia'

/** 全局唯一 Pinia 实例（供 app 与脱离组件的模块复用同一实例） */
export const pinia = createPinia()