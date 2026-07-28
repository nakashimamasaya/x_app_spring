import { useParams } from 'react-router'
import { problemMessage } from '../api/types'
import { Timeline } from '../components/Timeline'
import { useAuthContext } from '../features/auth/AuthContext'
import { useDeletePost, useUserPosts } from '../features/timeline/queries'
import { useFollow, useProfile } from '../features/user/queries'

export function ProfilePage() {
  const { username = '' } = useParams()
  const { state } = useAuthContext()
  const profile = useProfile(username)
  const posts = useUserPosts(username)
  const follow = useFollow(username)
  const deletePost = useDeletePost()

  if (profile.isPending) return <p className="p-4 text-gray-500">読み込み中…</p>
  if (profile.isError) {
    return (
      <p role="alert" className="p-4 text-red-600">
        {problemMessage(profile.error, 'プロフィールを取得できませんでした。')}
      </p>
    )
  }

  const user = profile.data
  const isSelf = state.status === 'authenticated' && state.user.id === user.id
  // 未認証では isFollowing が null（不明）。ボタン自体を出さない
  const canFollow = !isSelf && user.isFollowing !== null && user.isFollowing !== undefined

  return (
    <div>
      <header className="border-b border-gray-200 p-4">
        <h1 className="text-xl font-bold">{user.displayName}</h1>
        <p className="text-gray-500">@{user.username}</p>
        {user.bio && <p className="mt-2 whitespace-pre-wrap">{user.bio}</p>}

        <dl className="mt-3 flex gap-4 text-sm">
          <div><dt className="inline text-gray-500">投稿 </dt><dd className="inline font-bold">{user.postCount}</dd></div>
          <div><dt className="inline text-gray-500">フォロワー </dt><dd className="inline font-bold">{user.followerCount}</dd></div>
          <div><dt className="inline text-gray-500">フォロー中 </dt><dd className="inline font-bold">{user.followingCount}</dd></div>
        </dl>

        {canFollow && (
          <button
            type="button"
            onClick={() => follow.mutate(user.isFollowing ?? false)}
            aria-pressed={user.isFollowing ?? false}
            className={
              user.isFollowing
                ? 'mt-3 rounded border border-gray-400 px-4 py-1.5 text-sm'
                : 'mt-3 rounded bg-gray-900 px-4 py-1.5 text-sm text-white'
            }
          >
            {user.isFollowing ? 'フォロー中' : 'フォローする'}
          </button>
        )}
      </header>

      <Timeline
        query={posts}
        currentUserId={state.status === 'authenticated' ? state.user.id : undefined}
        onDelete={(post) => deletePost.mutate(post.id)}
        emptyMessage="まだ投稿がありません。"
      />
    </div>
  )
}
