import { defineConfig, devices } from '@playwright/test'

/**
 * E2E は既に起動しているスタックに対して実行する。
 * webServer は定義しない（compose が db / api / web を立てる責務を持つ）。
 *
 * `docker compose --profile e2e run --rm e2e npx playwright test`
 */
export default defineConfig({
  testDir: './tests',
  // 同じ DB を共有するので、並列実行するとデータが干渉する。
  // ユーザー名を一意にすれば緩和できるが、まずは確実性を優先する
  workers: 1,
  fullyParallel: false,
  // 落ちたテストだけ 1 回だけ再試行する。CI での一時的な失敗を拾いすぎない範囲で
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { outputFolder: '/output/playwright-report', open: 'never' }]],
  outputDir: '/output/test-results',

  use: {
    // コンテナ間通信なのでサービス名で解決する
    baseURL: process.env.E2E_BASE_URL ?? 'http://web:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'ja-JP',
  },

  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
