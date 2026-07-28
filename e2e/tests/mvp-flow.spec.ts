import { expect, test, type Page } from '@playwright/test'

/**
 * MVP の主要シナリオを通しで確認する。
 *
 * 単体・統合テストで各部品は検証済みなので、ここで見るのは
 * **ブラウザ越しに実際に繋がるか**。特に CORS と Cookie は
 * MockMvc では検証できない領域。
 */

/** 同じ DB を使い回すので、実行ごとに一意な名前を作る。 */
const unique = () => Math.random().toString(36).slice(2, 10)

async function register(page: Page, username: string) {
  await page.goto('/register')
  await page.getByLabel('ユーザー名').fill(username)
  await page.getByLabel('メールアドレス').fill(`${username}@example.com`)
  await page.getByLabel('パスワード').fill('password123')
  await page.getByLabel('表示名').fill(`${username} さん`)
  await page.getByRole('button', { name: '登録する' }).click()
  // 登録してもログイン状態にはならないので、ログイン画面へ遷移する
  await expect(page).toHaveURL(/\/login$/)
}

async function login(page: Page, username: string) {
  await page.goto('/login')
  await page.getByLabel('ユーザー名').fill(username)
  await page.getByLabel('パスワード').fill('password123')
  await page.getByRole('button', { name: 'ログイン' }).click()
  await expect(page).toHaveURL(/\/home$/)
}

test('登録してログインし、投稿がタイムラインに出る', async ({ page }) => {
  const user = `e2e${unique()}`
  await register(page, user)
  await login(page, user)

  const body = `E2E からの投稿 ${unique()} 🎉`
  await page.getByLabel('投稿内容').fill(body)
  await page.getByRole('button', { name: '投稿する' }).click()

  await expect(page.getByText(body)).toBeVisible()

  // 公開タイムラインにも出る
  await page.goto('/')
  await expect(page.getByText(body)).toBeVisible()
})

test('フォローすると相手の投稿がホームタイムラインに出る', async ({ page }) => {
  const author = `author${unique()}`
  const follower = `follower${unique()}`
  const body = `フォロー確認 ${unique()}`

  await register(page, author)
  await login(page, author)
  await page.getByLabel('投稿内容').fill(body)
  await page.getByRole('button', { name: '投稿する' }).click()
  await expect(page.getByText(body)).toBeVisible()
  await page.getByRole('button', { name: /ログアウト/ }).click()

  await register(page, follower)
  await login(page, follower)

  // フォロー前はホームに出ない
  await expect(page.getByText(body)).toHaveCount(0)

  await page.goto(`/users/${author}`)
  await page.getByRole('button', { name: 'フォローする' }).click()
  await expect(page.getByRole('button', { name: 'フォロー中' })).toBeVisible()

  await page.goto('/home')
  await expect(page.getByText(body)).toBeVisible()
})

test('いいねするとカウントが増え、取り消すと戻る', async ({ page }) => {
  const user = `liker${unique()}`
  const body = `いいね確認 ${unique()}`

  await register(page, user)
  await login(page, user)
  await page.getByLabel('投稿内容').fill(body)
  await page.getByRole('button', { name: '投稿する' }).click()
  await expect(page.getByText(body)).toBeVisible()

  const article = page.locator('article').filter({ hasText: body })
  await article.getByRole('button', { name: 'いいねする' }).click()
  await expect(article.getByRole('button', { name: 'いいねを取り消す' })).toContainText('1')

  await article.getByRole('button', { name: 'いいねを取り消す' }).click()
  await expect(article.getByRole('button', { name: 'いいねする' })).toContainText('0')
})

test('未認証ではホームに入れずログインへ飛ばされる', async ({ page }) => {
  await page.goto('/home')

  await expect(page).toHaveURL(/\/login$/)
})

test('未認証でも公開タイムラインは読めるが、いいねボタンは出ない', async ({ page }) => {
  const user = `anon${unique()}`
  const body = `未認証閲覧 ${unique()}`

  await register(page, user)
  await login(page, user)
  await page.getByLabel('投稿内容').fill(body)
  await page.getByRole('button', { name: '投稿する' }).click()
  await expect(page.getByText(body)).toBeVisible()
  await page.getByRole('button', { name: /ログアウト/ }).click()

  await page.goto('/')
  await expect(page.getByText(body)).toBeVisible()

  // likedByMe が null（不明）なのでボタンを出さない
  const article = page.locator('article').filter({ hasText: body })
  await expect(article.getByRole('button', { name: /いいね/ })).toHaveCount(0)
})

/**
 * Access Token をストレージに保存していないことを、実ブラウザで確認する。
 * BFF を挟まない構成の要件（docs/adr/0001）。
 */
test('Access Token が localStorage に保存されていない', async ({ page }) => {
  const user = `storage${unique()}`
  await register(page, user)
  await login(page, user)

  const storage = await page.evaluate(() => ({
    local: JSON.stringify(window.localStorage),
    session: JSON.stringify(window.sessionStorage),
  }))

  expect(storage.local).toBe('{}')
  expect(storage.session).toBe('{}')
})

/**
 * リロードすると Access Token（メモリ保持）は失われるが、
 * Refresh Cookie からサイレントに復帰できる。
 */
test('リロードしてもログイン状態が維持される', async ({ page }) => {
  const user = `reload${unique()}`
  await register(page, user)
  await login(page, user)

  await page.reload()

  await expect(page.getByRole('button', { name: new RegExp(user) })).toBeVisible()
})

/** Refresh Cookie は Path を /api/v1/auth に限定している。 */
test('Refresh Cookie が HttpOnly でパス限定されている', async ({ page, context }) => {
  const user = `cookie${unique()}`
  await register(page, user)
  await login(page, user)

  const cookies = await context.cookies()
  const refresh = cookies.find((c) => c.name === 'refresh_token')

  expect(refresh).toBeDefined()
  expect(refresh?.httpOnly).toBe(true)
  expect(refresh?.path).toBe('/api/v1/auth')
})
