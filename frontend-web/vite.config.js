import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { visualizer } from 'rollup-plugin-visualizer'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: false
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      dts: false
    }),
    visualizer()
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        silenceDeprecations: ['legacy-js-api', 'import', 'global-builtin'],
        additionalData: (source, filename) => {
          if (!filename || filename.includes('node_modules') || filename.includes('variables.scss')) {
            return source
          }
          return `@use "@/assets/styles/variables.scss" as *;\n${source}`
        }
      }
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080/api',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  build: {
    target: 'es2020',
    minify: 'terser',
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('echarts')) {
              if (id.includes('echarts/charts')) return 'echarts-charts'
              if (id.includes('echarts/components')) return 'echarts-components'
              if (id.includes('echarts/renderers')) return 'echarts-renderers'
              if (id.includes('echarts/core')) return 'echarts-core'
              return 'echarts'
            }
            if (id.includes('element-plus')) return 'element-plus'
            if (id.includes('vue-router')) return 'vue-router'
            if (id.includes('pinia')) return 'pinia'
            if (id.includes('axios')) return 'axios'
            if (id.includes('dayjs')) return 'dayjs'
            return 'vendor'
          }
        }
      }
    },
    cssCodeSplit: true,
    sourcemap: false
  },
  optimizeDeps: {
    include: ['element-plus', 'echarts', 'vue-router', 'pinia', 'axios', 'dayjs'],
    esbuildOptions: {
      target: 'es2020'
    }
  }
})
