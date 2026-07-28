import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { PostPage } from '../../api/types'

/**
 * クエリキーは 1 箇所に集約する。文字列を散らすと、
 * 無効化の対象を取りこぼして「投稿したのに一覧に出ない」が起きる。
 */
export const timelineKeys = {
  all: ['timeline'] as const,
  public: () => [...timelineKeys.all, 'public'] as const,
  home: () => [...timelineKeys.all, 'home'] as const,
  userPosts: (username: string) => ['userPosts', username] as const,
}

/** カーソルは不透明な文字列。前ページの nextCursor をそのまま渡す（docs/adr/0003）。 */
function pageParams() {
  return {
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (last: PostPage) => last.nextCursor ?? undefined,
  }
}

export function usePublicTimeline() {
  return useInfiniteQuery({
    queryKey: timelineKeys.public(),
    queryFn: async ({ pageParam }) => {
      const { data, error } = await api.GET('/timeline/public', {
        params: { query: { cursor: pageParam, limit: 20 } },
      })
      if (error || !data) throw error
      return data
    },
    ...pageParams(),
  })
}

export function useHomeTimeline(enabled: boolean) {
  return useInfiniteQuery({
    queryKey: timelineKeys.home(),
    enabled,
    queryFn: async ({ pageParam }) => {
      const { data, error } = await api.GET('/timeline/home', {
        params: { query: { cursor: pageParam, limit: 20 } },
      })
      if (error || !data) throw error
      return data
    },
    ...pageParams(),
  })
}

export function useUserPosts(username: string) {
  return useInfiniteQuery({
    queryKey: timelineKeys.userPosts(username),
    queryFn: async ({ pageParam }) => {
      const { data, error } = await api.GET('/users/{username}/posts', {
        params: { path: { username }, query: { cursor: pageParam, limit: 20 } },
      })
      if (error || !data) throw error
      return data
    },
    ...pageParams(),
  })
}

export function useCreatePost() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: string) => {
      const { data, error } = await api.POST('/posts', { body: { body } })
      if (error || !data) throw error
      return data
    },
    // 投稿は全てのタイムラインに影響しうるので、まとめて無効化する
    onSuccess: () => queryClient.invalidateQueries({ queryKey: timelineKeys.all }),
  })
}

export function useDeletePost() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (postId: string) => {
      const { error } = await api.DELETE('/posts/{postId}', {
        params: { path: { postId } },
      })
      if (error) throw error
    },
    onSuccess: () => queryClient.invalidateQueries(),
  })
}
