import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// 与前端-web 对齐：
// 前端请求 /api/xxx → 经 Vite proxy 去掉 /api → 再拼到 http://localhost:8080/api
// 最终到达后端的是 http://localhost:8080/api/xxx（匹配 context-path=/api）
export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5174,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080/api',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
