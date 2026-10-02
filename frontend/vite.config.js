import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// Локальная разработка: /api проксируется на бэкенд.
// По умолчанию — локальный бэкенд; на прод: VITE_PROXY_TARGET=https://fitcoach.keldari.online npm run dev
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  return {
    plugins: [vue()],
    server: {
      proxy: {
        '/api': {
          target,
          changeOrigin: true,
          secure: target.startsWith('https')
        }
      }
    }
  }
})
