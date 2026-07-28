// vitest/config の defineConfig を使わないと test プロパティが型エラーになる
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,
    port: 5173,
    // Vite は DNS リバインディング対策として Host ヘッダを検査し、
    // 許可外は 403 を返す。Docker のサービス名（http://web:5173）で
    // アクセスする E2E コンテナから到達できるよう明示的に許可する。
    // 開発サーバー専用の設定で、本番の nginx 配信には影響しない。
    allowedHosts: ['localhost', '127.0.0.1', 'web', 'host.docker.internal'],
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
