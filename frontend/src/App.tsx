import type { ReactNode } from 'react'
import { Link, Navigate, Route, Routes, useLocation } from 'react-router'
import { AuthProvider, useAuthContext } from './features/auth/AuthContext'
import { HomeTimelinePage } from './routes/HomeTimelinePage'
import { LoginPage } from './routes/LoginPage'
import { PostDetailPage } from './routes/PostDetailPage'
import { ProfilePage } from './routes/ProfilePage'
import { PublicTimelinePage } from './routes/PublicTimelinePage'
import { RegisterPage } from './routes/RegisterPage'

/**
 * 認証が要る画面のガード。
 *
 * 起動直後の loading を anonymous と区別しないと、リロードのたびに
 * ログイン画面が一瞬見えてしまう。
 */
function RequireAuth({ children }: { children: ReactNode }) {
  const { state } = useAuthContext()
  const location = useLocation()

  if (state.status === 'loading') {
    return <p className="p-4 text-gray-500">読み込み中…</p>
  }
  if (state.status === 'anonymous') {
    // ログイン後に元の画面へ戻せるよう、来た場所を渡す
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  return <>{children}</>
}

function Header() {
  const { state, logout } = useAuthContext()

  return (
    <header className="border-b border-gray-200">
      <nav className="mx-auto flex max-w-2xl items-center gap-4 p-4">
        <Link to="/" className="font-bold">
          x_app_spring
        </Link>
        {state.status === 'authenticated' ? (
          <>
            <Link to="/home" className="text-sm hover:underline">
              ホーム
            </Link>
            <Link to={`/users/${state.user.username}`} className="text-sm hover:underline">
              プロフィール
            </Link>
            <button
              type="button"
              onClick={() => void logout()}
              className="ml-auto text-sm text-gray-600 hover:underline"
            >
              ログアウト（@{state.user.username}）
            </button>
          </>
        ) : (
          <Link to="/login" className="ml-auto text-sm hover:underline">
            ログイン
          </Link>
        )}
      </nav>
    </header>
  )
}

export function App() {
  return (
    <AuthProvider>
      <Header />
      <main className="mx-auto max-w-2xl">
        <Routes>
          <Route path="/" element={<PublicTimelinePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/posts/:postId" element={<PostDetailPage />} />
          <Route path="/users/:username" element={<ProfilePage />} />
          <Route
            path="/home"
            element={
              <RequireAuth>
                <HomeTimelinePage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<p className="p-4 text-gray-500">ページが見つかりません。</p>} />
        </Routes>
      </main>
    </AuthProvider>
  )
}
