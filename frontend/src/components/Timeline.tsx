import type { UseInfiniteQueryResult } from '@tanstack/react-query'
import type { Post, PostPage } from '../api/types'
import { problemMessage } from '../api/types'
import { useLike } from '../features/post/useLike'
import { PostCard } from './PostCard'

type Props = {
  query: UseInfiniteQueryResult<{ pages: PostPage[] }, unknown>
  currentUserId?: string
  onDelete?: (post: Post) => void
  emptyMessage?: string
}

/**
 * 無限スクロールのタイムライン。カーソルページングなので
 * 「次があるか」は nextCursor の有無で決まり、総件数は分からない（docs/adr/0003）。
 */
export function Timeline({
  query,
  currentUserId,
  onDelete,
  emptyMessage = 'まだ投稿がありません。',
}: Props) {
  const like = useLike()

  if (query.isPending) {
    return <p className="p-4 text-gray-500">読み込み中…</p>
  }

  if (query.isError) {
    return (
      <p role="alert" className="p-4 text-red-600">
        {problemMessage(query.error, 'タイムラインを取得できませんでした。')}
      </p>
    )
  }

  const posts = query.data.pages.flatMap((page) => page.items)

  if (posts.length === 0) {
    return <p className="p-4 text-gray-500">{emptyMessage}</p>
  }

  return (
    <div>
      {posts.map((post) => (
        <PostCard
          key={post.id}
          post={post}
          canDelete={currentUserId !== undefined && post.author.id === currentUserId}
          onDelete={onDelete}
          onToggleLike={(target) =>
            like.mutate({ postId: target.id, liked: target.likedByMe ?? false })
          }
        />
      ))}

      {query.hasNextPage && (
        <div className="p-4 text-center">
          <button
            type="button"
            onClick={() => void query.fetchNextPage()}
            disabled={query.isFetchingNextPage}
            className="rounded border border-gray-300 px-4 py-2 text-sm disabled:opacity-50"
          >
            {query.isFetchingNextPage ? '読み込み中…' : 'もっと見る'}
          </button>
        </div>
      )}
    </div>
  )
}
