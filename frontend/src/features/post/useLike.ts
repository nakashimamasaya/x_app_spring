import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { Post, PostPage } from '../../api/types'

type Cached = { pages: PostPage[]; pageParams: unknown[] }

/**
 * いいねの付け外し。
 *
 * 楽観的更新を行う。API は冪等なので（docs/adr/0005）、
 * 連打や再送でサーバー側の状態が壊れる心配がなく、
 * 先に画面を更新してしまってよい。
 */
export function useLike() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ postId, liked }: { postId: string; liked: boolean }) => {
      const params = { params: { path: { postId } } }
      const { error } = liked
        ? await api.DELETE('/posts/{postId}/like', params)
        : await api.POST('/posts/{postId}/like', params)
      if (error) throw error
    },

    onMutate: async ({ postId, liked }) => {
      // 進行中の取得が楽観的更新を上書きしないよう止める
      await queryClient.cancelQueries()
      const snapshot = queryClient.getQueriesData({ queryKey: [] })

      const patch = (post: Post): Post =>
        post.id === postId
          ? { ...post, likedByMe: !liked, likeCount: post.likeCount + (liked ? -1 : 1) }
          : post

      // 一覧（無限クエリ）と単体取得の両方を更新する
      queryClient.setQueriesData<Cached>({ queryKey: ['timeline'] }, (old) =>
        old ? { ...old, pages: old.pages.map((p) => ({ ...p, items: p.items.map(patch) })) } : old,
      )
      queryClient.setQueriesData<Cached>({ queryKey: ['userPosts'] }, (old) =>
        old ? { ...old, pages: old.pages.map((p) => ({ ...p, items: p.items.map(patch) })) } : old,
      )
      queryClient.setQueriesData<Post>({ queryKey: ['post', postId] }, (old) =>
        old ? patch(old) : old,
      )

      return { snapshot }
    },

    // 失敗したら楽観的更新を巻き戻す。巻き戻さないと画面とサーバーがズレたままになる
    onError: (_error, _variables, context) => {
      context?.snapshot.forEach(([key, data]) => queryClient.setQueryData(key, data))
    },

    onSettled: () => queryClient.invalidateQueries(),
  })
}
