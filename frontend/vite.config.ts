// vitest/config の defineConfig を使わないと test プロパティが型エラーになる
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,
    port: 5173,
    watch: {
      // macOS の bind mount では inotify がコンテナに届かず HMR が反応しない。
      // ポーリングに切り替えないと、ファイルを保存しても画面が更新されない。
      usePolling: true,
      interval: 300,
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
