import { PostComposer } from '../components/PostComposer'
import { Timeline } from '../components/Timeline'
import { useAuthContext } from '../features/auth/AuthContext'
import { useDeletePost, useHomeTimeline } from '../features/timeline/queries'

/** ホームタイムライン。自分とフォロー中の投稿だけが並ぶ。認証必須。 */
export function HomeTimelinePage() {
  const { state } = useAuthContext()
  const authenticated = state.status === 'authenticated'
  const query = useHomeTimeline(authenticated)
  const deletePost = useDeletePost()

  return (
    <div>
      <h1 className="p-4 text-xl font-bold">ホーム</h1>
      <PostComposer />
      <Timeline
        query={query}
        currentUserId={authenticated ? state.user.id : undefined}
        onDelete={(post) => deletePost.mutate(post.id)}
        emptyMessage="まだ投稿がありません。誰かをフォローするか、自分で投稿してみましょう。"
      />
    </div>
  )
}
