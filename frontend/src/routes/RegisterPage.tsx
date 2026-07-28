import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { api } from '../api/client'
import { fieldErrors, problemMessage } from '../api/types'

/** 登録してもログイン状態にはならないので、成功後はログイン画面へ送る。 */
export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', password: '', displayName: '' })
  const [errors, setErrors] = useState<{ field: string; message: string }[]>([])
  const [message, setMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const update = (key: keyof typeof form) => (value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const fields = [
    { key: 'username', label: 'ユーザー名', type: 'text', hint: '英数字とアンダースコア 3〜20 文字' },
    { key: 'email', label: 'メールアドレス', type: 'email', hint: '' },
    { key: 'password', label: 'パスワード', type: 'password', hint: '8 文字以上' },
    { key: 'displayName', label: '表示名', type: 'text', hint: '' },
  ] as const

  return (
    <form
      className="mx-auto max-w-sm p-4"
      onSubmit={async (event) => {
        event.preventDefault()
        setSubmitting(true)
        setErrors([])
        setMessage(null)
        const { error } = await api.POST('/auth/register', { body: form })
        setSubmitting(false)
        if (error) {
          setErrors(fieldErrors(error))
          setMessage(problemMessage(error, '登録できませんでした。'))
          return
        }
        void navigate('/login')
      }}
    >
      <h1 className="text-xl font-bold">新規登録</h1>

      {fields.map(({ key, label, type, hint }) => (
        <div key={key}>
          <label htmlFor={`register-${key}`} className="mt-3 block text-sm">
            {label}
          </label>
          <input
            id={`register-${key}`}
            type={type}
            value={form[key]}
            onChange={(e) => update(key)(e.target.value)}
            className="w-full rounded border border-gray-300 p-2"
          />
          {hint && <p className="text-xs text-gray-500">{hint}</p>}
        </div>
      ))}

      <button
        type="submit"
        disabled={submitting}
        className="mt-4 w-full rounded bg-gray-900 px-4 py-2 text-white disabled:opacity-40"
      >
        {submitting ? '登録中…' : '登録する'}
      </button>

      {message && (
        <div role="alert" className="mt-3 text-sm text-red-600">
          <p>{message}</p>
          <ul className="list-disc pl-5">
            {errors.map((e) => (
              <li key={e.field}>{e.message}</li>
            ))}
          </ul>
        </div>
      )}

      <p className="mt-4 text-sm text-gray-600">
        既にアカウントがある場合は{' '}
        <Link to="/login" className="underline">
          ログイン
        </Link>
      </p>
    </form>
  )
}
