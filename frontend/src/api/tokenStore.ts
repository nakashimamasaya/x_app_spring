/**
 * Access Token の保管場所。
 *
 * **モジュールスコープの変数にのみ保持し、localStorage / sessionStorage には保存しない。**
 * BFF を挟まない構成（docs/adr/0001）では XSS がそのままトークン奪取につながるため、
 * 被害範囲をタブのライフタイムに限定する。
 *
 * リロードすると失われるが、Refresh Cookie から再取得できるので問題にならない。
 */
let accessToken: string | null = null

/** トークンが変わったことを React 側に知らせるための購読。 */
type Listener = (token: string | null) => void
const listeners = new Set<Listener>()

export const tokenStore = {
  get(): string | null {
    return accessToken
  },

  set(token: string | null): void {
    accessToken = token
    listeners.forEach((listener) => listener(token))
  },

  clear(): void {
    tokenStore.set(null)
  },

  subscribe(listener: Listener): () => void {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
}
