import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { problemMessage } from '../api/types'
import { useAuthContext } from '../features/auth/AuthContext'

export function LoginPage() {
  const { login } = useAuthContext()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  return (
    <form
      className="mx-auto max-w-sm p-4"
      onSubmit={async (event) => {
        event.preventDefault()
        setSubmitting(true)
        setError(null)
        const result = await login(username, password)
        setSubmitting(false)
        if (result.ok) {
          void navigate('/home')
        } else {
          // サーバーは username 不在とパスワード誤りを区別しない（ユーザー列挙対策）。
          // 画面でも区別して見せない
          setError(problemMessage(result.problem, 'ログインできませんでした。'))
        }
      }}
    >
      <h1 className="text-xl font-bold">ログイン</h1>

      <label htmlFor="login-username" className="mt-4 block text-sm">
        ユーザー名
      </label>
      <input
        id="login-username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        autoComplete="username"
        className="w-full rounded border border-gray-300 p-2"
      />

      <label htmlFor="login-password" className="mt-3 block text-sm">
        パスワード
      </label>
      <input
        id="login-password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        autoComplete="current-password"
        className="w-full rounded border border-gray-300 p-2"
      />

      <button
        type="submit"
        disabled={submitting}
        className="mt-4 w-full rounded bg-gray-900 px-4 py-2 text-white disabled:opacity-40"
      >
        {submitting ? 'ログイン中…' : 'ログイン'}
      </button>

      {error && (
        <p role="alert" className="mt-3 text-sm text-red-600">
          {error}
        </p>
      )}

      <p className="mt-4 text-sm text-gray-600">
        アカウントが無い場合は{' '}
        <Link to="/register" className="underline">
          新規登録
        </Link>
      </p>
    </form>
  )
}
