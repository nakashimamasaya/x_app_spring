import { useCallback, useEffect, useState } from 'react'
import { api, tokenStore } from '../../api/client'
import type { components } from '../../api/generated/schema'

type User = components['schemas']['User']

/**
 * 認証状態。
 *
 * `status` を 3 状態にしているのは、起動直後の「まだ分からない」を
 * 「未ログイン」と区別するため。区別しないと、リロードのたびに
 * ログイン画面が一瞬表示されてしまう。
 */
export type AuthState =
  | { status: 'loading' }
  | { status: 'authenticated'; user: User }
  | { status: 'anonymous' }

export function useAuth() {
  const [state, setState] = useState<AuthState>({ status: 'loading' })

  const loadMe = useCallback(async () => {
    const { data, error } = await api.GET('/users/me')
    if (error || !data) {
      setState({ status: 'anonymous' })
      return
    }
    setState({ status: 'authenticated', user: data })
  }, [])

  // 起動時に Refresh Cookie からセッションを復元する。
  // Access Token はメモリ保持なのでリロードで失われるが、
  // クライアントの middleware が 401 を受けて自動でリフレッシュする。
  useEffect(() => {
    void loadMe()
    return tokenStore.subscribe((token) => {
      if (token === null) {
        setState({ status: 'anonymous' })
      }
    })
  }, [loadMe])

  const login = useCallback(
    async (username: string, password: string) => {
      const { data, error } = await api.POST('/auth/login', {
        body: { username, password },
      })
      if (error || !data) {
        return { ok: false as const, problem: error }
      }
      tokenStore.set(data.accessToken)
      await loadMe()
      return { ok: true as const }
    },
    [loadMe],
  )

  const logout = useCallback(async () => {
    await api.POST('/auth/logout')
    tokenStore.clear()
    setState({ status: 'anonymous' })
  }, [])

  return { state, login, logout, reload: loadMe }
}
