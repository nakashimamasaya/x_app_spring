import { createContext, useContext, type ReactNode } from 'react'
import { useAuth } from './useAuth'

type AuthValue = ReturnType<typeof useAuth> & {
  /**
   * 認証状態が確定したか。
   *
   * これを待たずにデータ取得を始めると、リロード直後は Access Token が
   * まだ復元されておらず、認証済みなのに未認証としてレスポンスが返る
   * （isFollowing / likedByMe が null になる）。
   */
  ready: boolean
}

const AuthContext = createContext<AuthValue | null>(null)

/**
 * 認証状態はアプリ全体で 1 つ。各コンポーネントが useAuth を直接呼ぶと
 * それぞれが /users/me を叩き、起動時に同じリクエストが何本も飛ぶ。
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const auth = useAuth()
  const value: AuthValue = { ...auth, ready: auth.state.status !== 'loading' }
  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuthContext(): AuthValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuthContext は AuthProvider の内側でのみ使えます')
  }
  return value
}
