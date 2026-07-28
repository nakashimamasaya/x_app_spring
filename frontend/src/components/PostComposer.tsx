import { useState } from 'react'
import { fieldErrors, problemMessage } from '../api/types'
import { useCreatePost } from '../features/timeline/queries'

const MAX_CODE_POINTS = 280

/**
 * 文字数は**コードポイント数**で数える（INV-6）。
 * `body.length` は UTF-16 単位なので絵文字が 2 文字になり、
 * サーバー側の判定とズレて「画面上は 280 以内なのに 400 が返る」ことになる。
 */
function countCodePoints(text: string): number {
  return [...text].length
}

export function PostComposer() {
  const [body, setBody] = useState('')
  const createPost = useCreatePost()

  const length = countCodePoints(body.trim())
  const tooLong = length > MAX_CODE_POINTS
  const canSubmit = length > 0 && !tooLong && !createPost.isPending

  const errors = fieldErrors(createPost.error)

  return (
    <form
      className="border-b border-gray-200 p-4"
      onSubmit={(event) => {
        event.preventDefault()
        if (!canSubmit) return
        createPost.mutate(body, { onSuccess: () => setBody('') })
      }}
    >
      <label htmlFor="post-body" className="sr-only">
        投稿内容
      </label>
      <textarea
        id="post-body"
        value={body}
        onChange={(event) => setBody(event.target.value)}
        rows={3}
        placeholder="いまどうしてる？"
        className="w-full resize-none rounded border border-gray-300 p-2"
      />

      <div className="mt-2 flex items-center gap-3">
        <span className={tooLong ? 'text-sm text-red-600' : 'text-sm text-gray-500'}>
          {length} / {MAX_CODE_POINTS}
        </span>
        <button
          type="submit"
          disabled={!canSubmit}
          className="ml-auto rounded bg-gray-900 px-4 py-1.5 text-white disabled:opacity-40"
        >
          {createPost.isPending ? '投稿中…' : '投稿する'}
        </button>
      </div>

      {createPost.isError && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {errors.length > 0
            ? errors.map((e) => e.message).join(' / ')
            : problemMessage(createPost.error, '投稿できませんでした。')}
        </p>
      )}
    </form>
  )
}
