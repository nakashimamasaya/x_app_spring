import { Link } from 'react-router'
import type { Post } from '../api/types'

type Props = {
  post: Post
  onToggleLike?: (post: Post) => void
  onDelete?: (post: Post) => void
  canDelete?: boolean
}

/** 日時はブラウザのロケールで表示する。サーバーは常に UTC の ISO 8601 を返す。 */
function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function PostCard({ post, onToggleLike, onDelete, canDelete = false }: Props) {
  // 未認証では null（不明）。その場合はいいねボタンを出さない
  const canLike = post.likedByMe !== null && post.likedByMe !== undefined

  return (
    <article className="border-b border-gray-200 p-4">
      <header className="flex items-baseline gap-2 text-sm">
        <Link
          to={`/users/${post.author.username}`}
          className="font-bold text-gray-900 hover:underline"
        >
          {post.author.displayName}
        </Link>
        <span className="text-gray-500">@{post.author.username}</span>
        <time dateTime={post.createdAt} className="ml-auto text-gray-500">
          {formatTime(post.createdAt)}
        </time>
      </header>

      <Link to={`/posts/${post.id}`} className="mt-2 block whitespace-pre-wrap break-words">
        {post.body}
      </Link>

      <footer className="mt-3 flex items-center gap-4 text-sm">
        {canLike ? (
          <button
            type="button"
            onClick={() => onToggleLike?.(post)}
            aria-pressed={post.likedByMe ?? false}
            aria-label={post.likedByMe ? 'いいねを取り消す' : 'いいねする'}
            className={
              post.likedByMe
                ? 'rounded px-2 py-1 font-medium text-rose-600'
                : 'rounded px-2 py-1 text-gray-500 hover:text-rose-600'
            }
          >
            ♥ {post.likeCount}
          </button>
        ) : (
          <span className="px-2 py-1 text-gray-500">♥ {post.likeCount}</span>
        )}

        {canDelete && (
          <button
            type="button"
            onClick={() => onDelete?.(post)}
            className="ml-auto rounded px-2 py-1 text-gray-500 hover:text-red-600"
          >
            削除
          </button>
        )}
      </footer>
    </article>
  )
}
