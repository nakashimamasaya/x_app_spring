import createClient, { type Middleware } from 'openapi-fetch'
import type { paths } from './generated/schema'
import { tokenStore } from './tokenStore'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

/** Refresh Cookie は Path=/api/v1/auth に限定されているため、この判定で足りる。 */
const isAuthPath = (url: string) => new URL(url, BASE_URL).pathname.includes('/auth/')

/**
 * 同時に複数のリクエストが 401 になっても、リフレッシュは 1 回だけ走らせる。
 * これが無いと Refresh Token のローテーションが競合し、
 * 「失効済みトークンの再利用」と誤検知されて全セッションが切られる（INV-10）。
 */
let refreshing: Promise<boolean> | null = null

async function refreshAccessToken(): Promise<boolean> {
  refreshing ??= (async () => {
    try {
      const response = await fetch(`${BASE_URL}/auth/refresh`, {
        method: 'POST',
        // Refresh Cookie を送るために必須
        credentials: 'include',
      })
      if (!response.ok) {
        tokenStore.clear()
        return false
      }
      const body = (await response.json()) as { accessToken: string }
      tokenStore.set(body.accessToken)
      return true
    } catch {
      tokenStore.clear()
      return false
    } finally {
      refreshing = null
    }
  })()
  return refreshing
}

const authMiddleware: Middleware = {
  async onRequest({ request }) {
    const token = tokenStore.get()
    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`)
    }
    return request
  },

  async onResponse({ request, response }) {
    // /auth/* の 401 はリフレッシュ対象にしない。ログイン失敗やリフレッシュ自体の
    // 失敗まで再試行すると無限ループになる
    if (response.status !== 401 || isAuthPath(request.url)) {
      return response
    }

    const refreshed = await refreshAccessToken()
    if (!refreshed) {
      return response
    }

    // 新しいトークンで 1 度だけ再試行する
    const retry = request.clone()
    retry.headers.set('Authorization', `Bearer ${tokenStore.get()}`)
    return fetch(retry)
  },
}

export const api = createClient<paths>({
  baseUrl: BASE_URL,
  // Refresh Cookie の送受信に必要
  credentials: 'include',
  // openapi-fetch は生成時に fetch の参照を捕捉する。ここで束縛せず
  // 呼び出しのたびに globalThis から引くことで、テストで差し替えられるようにする。
  fetch: (request) => globalThis.fetch(request),
})

api.use(authMiddleware)

/** ログイン・リフレッシュ以外では使わない。Access Token の保管は tokenStore に集約する。 */
export { tokenStore }
