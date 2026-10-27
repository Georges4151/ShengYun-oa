import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// CEAMS Web 构建配置
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // 占位：对接后端公网 HTTPS（取消 VPN）
      '/api': {
        target: 'https://api.example.com',
        changeOrigin: true,
        secure: true
      }
    }
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 1500
  }
})