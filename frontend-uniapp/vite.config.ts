import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// uni-app 3.x Vite 配置
// manifest.json 和 pages.json 在根目录（非 src/）
export default defineConfig({
  plugins: [uni()],
  // 确保 UniApp 从根目录加载配置文件
  root: __dirname,
})
