import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// uni-app（微信小程序）构建配置
export default defineConfig({
  plugins: [uni()]
})