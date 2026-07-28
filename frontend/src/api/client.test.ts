import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, tokenStore } from './client'

/**
 * middleware の振る舞いを検証する。fetch をスタブして、
 * 実際のリクエストの並びとヘッダを観察する。
 */
describe('api client middleware', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  const json = (body: unknown, status = 200) =>
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    })

  beforeEach(() => {
    tokenStore.clear()
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('トークンがあれば Authorization ヘッダを付ける', async () => {
    tokenStore.set('token-abc')
    fetchMock.mockResolvedValue(json({ items: [], nextCursor: null }))

    await api.GET('/timeline/public')

    const request = fetchMock.mock.calls[0][0] as Request
    expect(request.headers.get('Authorization')).toBe('Bearer token-abc')
  })

  it('トークンが無ければ Authorization ヘッダを付けない', async () => {
    fetchMock.mockResolvedValue(json({ items: [], nextCursor: null }))

    await api.GET('/timeline/public')

    const request = fetchMock.mock.calls[0][0] as Request
    expect(request.headers.get('Authorization')).toBeNull()
  })

  it('401 を受けるとリフレッシュして元のリクエストを再試行する', async () => {
    tokenStore.set('expired-token')
    fetchMock
      .mockResolvedValueOnce(json({ title: 'Unauthorized' }, 401))
      .mockResolvedValueOnce(json({ accessToken: 'fresh-token', tokenType: 'Bearer', expiresIn: 900 }))
      .mockResolvedValueOnce(json({ id: 'u1', username: 'alice' }))

    await api.GET('/users/me')

    expect(fetchMock).toHaveBeenCalledTimes(3)
    const refreshCall = fetchMock.mock.calls[1][0] as string
    expect(String(refreshCall)).toContain('/auth/refresh')
    // 再試行は新しいトークンで行う
    const retry = fetchMock.mock.calls[2][0] as Request
    expect(retry.headers.get('Authorization')).toBe('Bearer fresh-token')
  })

  /**
   * これが無いと、ログイン失敗の 401 でリフレッシュが走り、
   * さらにその失敗で再びリフレッシュ…と無限ループになる。
   */
  it('auth 配下の 401 ではリフレッシュしない', async () => {
    fetchMock.mockResolvedValue(json({ title: 'Invalid credentials' }, 401))

    await api.POST('/auth/login', { body: { username: 'alice', password: 'wrongpass' } })

    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('リフレッシュに失敗したらトークンを破棄する', async () => {
    tokenStore.set('expired-token')
    fetchMock
      .mockResolvedValueOnce(json({ title: 'Unauthorized' }, 401))
      .mockResolvedValueOnce(json({ title: 'Refresh token revoked' }, 401))

    await api.GET('/users/me')

    expect(tokenStore.get()).toBeNull()
  })

  /**
   * 同時に複数のリクエストが 401 になってもリフレッシュは 1 回だけ。
   * 複数回走ると Refresh Token のローテーションが競合し、
   * バックエンドが「失効済みトークンの再利用」と誤検知して
   * 全セッションを切ってしまう（INV-10）。
   */
  it('同時に401が起きてもリフレッシュは1回だけ走る', async () => {
    tokenStore.set('expired-token')
    fetchMock.mockImplementation((input: Request | string) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.includes('/auth/refresh')) {
        return Promise.resolve(
          json({ accessToken: 'fresh-token', tokenType: 'Bearer', expiresIn: 900 }),
        )
      }
      if (tokenStore.get() === 'expired-token') {
        return Promise.resolve(json({ title: 'Unauthorized' }, 401))
      }
      return Promise.resolve(json({ items: [], nextCursor: null }))
    })

    await Promise.all([
      api.GET('/timeline/home'),
      api.GET('/users/me'),
      api.GET('/timeline/public'),
    ])

    const refreshCalls = fetchMock.mock.calls.filter((call) => {
      const input = call[0] as Request | string
      const url = typeof input === 'string' ? input : input.url
      return url.includes('/auth/refresh')
    })
    expect(refreshCalls).toHaveLength(1)
  })
})
