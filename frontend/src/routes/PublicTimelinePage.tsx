import { PostComposer } from '../components/PostComposer'
import { Timeline } from '../components/Timeline'
import { useAuthContext } from '../features/auth/AuthContext'
import { usePublicTimeline, useDeletePost } from '../features/timeline/queries'

/** 公開タイムライン。未認証でも読める（api/openapi.yaml の security: []）。 */
export function PublicTimelinePage() {
  const { state } = useAuthContext()
  const query = usePublicTimeline()
  const deletePost = useDeletePost()
  const currentUserId = state.status === 'authenticated' ? state.user.id : undefined

  return (
    <div>
      <h1 className="p-4 text-xl font-bold">みんなの投稿</h1>
      {state.status === 'authenticated' && <PostComposer />}
      <Timeline
        query={query}
        currentUserId={currentUserId}
        onDelete={(post) => deletePost.mutate(post.id)}
      />
    </div>
  )
}
