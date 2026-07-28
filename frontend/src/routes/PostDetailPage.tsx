import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router'
import { api } from '../api/client'
import { problemMessage } from '../api/types'
import { PostCard } from '../components/PostCard'
import { useAuthContext } from '../features/auth/AuthContext'
import { useLike } from '../features/post/useLike'
import { useDeletePost } from '../features/timeline/queries'

export function PostDetailPage() {
  const { postId = '' } = useParams()
  const navigate = useNavigate()
  const { state, ready } = useAuthContext()
  const like = useLike()
  const deletePost = useDeletePost()

  const query = useQuery({
    queryKey: ['post', postId],
    // 認証状態が確定してから取得する（未確定だと likedByMe が null で返る）
    enabled: ready,
    queryFn: async () => {
      const { data, error } = await api.GET('/posts/{postId}', {
        params: { path: { postId } },
      })
      if (error || !data) throw error
      return data
    },
  })

  if (query.isPending) return <p className="p-4 text-gray-500">読み込み中…</p>
  if (query.isError) {
    return (
      <p role="alert" className="p-4 text-red-600">
        {problemMessage(query.error, '投稿を取得できませんでした。')}
      </p>
    )
  }

  const post = query.data
  const isAuthor = state.status === 'authenticated' && state.user.id === post.author.id

  return (
    <PostCard
      post={post}
      canDelete={isAuthor}
      onToggleLike={(target) =>
        like.mutate({ postId: target.id, liked: target.likedByMe ?? false })
      }
      onDelete={(target) =>
        deletePost.mutate(target.id, { onSuccess: () => void navigate('/') })
      }
    />
  )
}
