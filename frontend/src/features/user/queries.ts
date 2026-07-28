import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { UserProfile } from '../../api/types'

export const userKeys = {
  profile: (username: string) => ['profile', username] as const,
}

export function useProfile(username: string, enabled: boolean) {
  return useQuery({
    queryKey: userKeys.profile(username),
    enabled,
    queryFn: async () => {
      const { data, error } = await api.GET('/users/{username}', {
        params: { path: { username } },
      })
      if (error || !data) throw error
      return data
    },
  })
}

/**
 * フォローの切り替え。いいねと同じく API が冪等なので楽観的更新を行う。
 *
 * 未認証時 `isFollowing` は null（不明）。その状態でこの操作は呼ばれない
 * （ボタン自体を出さない）。
 */
export function useFollow(username: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (following: boolean) => {
      const params = { params: { path: { username } } }
      const { error } = following
        ? await api.DELETE('/users/{username}/follow', params)
        : await api.POST('/users/{username}/follow', params)
      if (error) throw error
    },

    onMutate: async (following) => {
      await queryClient.cancelQueries({ queryKey: userKeys.profile(username) })
      const previous = queryClient.getQueryData<UserProfile>(userKeys.profile(username))

      queryClient.setQueryData<UserProfile>(userKeys.profile(username), (old) =>
        old
          ? {
              ...old,
              isFollowing: !following,
              followerCount: old.followerCount + (following ? -1 : 1),
            }
          : old,
      )
      return { previous }
    },

    onError: (_error, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(userKeys.profile(username), context.previous)
      }
    },

    // フォロー状態はホームタイムラインの内容も変えるので全体を無効化する
    onSettled: () => queryClient.invalidateQueries(),
  })
}
