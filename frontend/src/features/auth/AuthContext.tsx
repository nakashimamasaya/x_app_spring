import { createContext, useContext, type ReactNode } from 'react'
import { useAuth } from './useAuth'

type AuthValue = ReturnType<typeof useAuth>

const AuthContext = createContext<AuthValue | null>(null)

/**
 * 認証状態はアプリ全体で 1 つ。各コンポーネントが useAuth を直接呼ぶと
 * それぞれが /users/me を叩き、起動時に同じリクエストが何本も飛ぶ。
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const value = useAuth()
  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuthContext(): AuthValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuthContext は AuthProvider の内側でのみ使えます')
  }
  return value
}
