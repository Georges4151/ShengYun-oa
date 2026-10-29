import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// CEAMS Web 构建配置
export default defineConfig(({ mode }) => {
  // 第三个参数传 '' 表示不做 VITE_ 前缀过滤，便于读取 VITE_PROXY_TARGET 等自定义变量
  const env = loadEnv(mode, process.cwd(), '')
  // 本地开发默认代理到本机后端；生产构建不启用 dev server，proxy 不参与打包
  const proxyTarget = env.VITE_PROXY_TARGET ?? 'http://localhost:8080'

  return {
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
        // 本地联调：/api -> 本机后端（8080），可用 VITE_PROXY_TARGET 覆盖
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          // http 目标必须关闭 secure 校验，仅 https 目标启用
          secure: proxyTarget.startsWith('https:')
        }
      }
    },
    build: {
      sourcemap: false,
      chunkSizeWarningLimit: 1500
    }
  }
})
