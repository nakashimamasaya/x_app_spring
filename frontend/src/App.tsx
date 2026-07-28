import { useAuth } from './features/auth/useAuth'

/**
 * 疎通確認用の最小画面。各機能の画面は次の PR で作る。
 * ここでは認証状態の 3 分岐が正しく動くことだけを示す。
 */
export function App() {
  const { state, logout } = useAuth()

  if (state.status === 'loading') {
    return <p className="p-4 text-gray-500">読み込み中…</p>
  }

  return (
    <main className="mx-auto max-w-2xl p-4">
      <h1 className="text-2xl font-bold">x_app_spring</h1>
      {state.status === 'authenticated' ? (
        <div className="mt-4">
          <p>
            ログイン中: <strong>{state.user.username}</strong>
          </p>
          <button
            type="button"
            onClick={() => void logout()}
            className="mt-2 rounded bg-gray-800 px-3 py-1 text-white"
          >
            ログアウト
          </button>
        </div>
      ) : (
        <p className="mt-4 text-gray-600">ログインしていません。</p>
      )}
    </main>
  )
}
