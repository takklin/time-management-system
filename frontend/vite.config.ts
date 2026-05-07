import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  optimizeDeps: {
    include: ['@element-plus/icons-vue'],
    force: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // 本地开发后端（默认指向本地 Spring Boot，已切换为 8081）
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
      },
    },
  },
})
