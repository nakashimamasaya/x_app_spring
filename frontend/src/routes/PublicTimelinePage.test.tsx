import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { tokenStore } from '../api/client'
import { AuthProvider } from '../features/auth/AuthContext'
import { API_BASE, server } from '../test/server'
import { renderWithProviders } from '../test/renderWithProviders'
import { PublicTimelinePage } from './PublicTimelinePage'

/** レスポンスの形は api/openapi.yaml の examples に合わせる。 */
const post = (id: string, body: string, likedByMe: boolean | null = null) => ({
  id,
  author: { id: 'author-1', username: 'alice', displayName: 'アリス' },
  body,
  createdAt: '2026-07-28T09:30:00Z',
  likeCount: 0,
  likedByMe,
})

const renderPage = () =>
  renderWithProviders(
    <AuthProvider>
      <PublicTimelinePage />
    </AuthProvider>,
  )

describe('PublicTimelinePage', () => {
  beforeEach(() => {
    tokenStore.clear()
  })

  it('未認証では投稿一覧を表示し、投稿フォームは出さない', async () => {
    server.use(
      // 未認証なので /users/me は 401。middleware がリフレッシュを試み、それも 401
      http.get(`${API_BASE}/users/me`, () => HttpResponse.json({}, { status: 401 })),
      http.post(`${API_BASE}/auth/refresh`, () => HttpResponse.json({}, { status: 401 })),
      http.get(`${API_BASE}/timeline/public`, () =>
        HttpResponse.json({ items: [post('p1', '公開投稿です')], nextCursor: null }),
      ),
    )

    renderPage()

    expect(await screen.findByText('公開投稿です')).toBeInTheDocument()
    expect(screen.queryByRole('textbox', { name: '投稿内容' })).not.toBeInTheDocument()
  })

  it('認証済みなら投稿フォームを表示する', async () => {
    tokenStore.set('valid-token')
    server.use(
      http.get(`${API_BASE}/users/me`, () =>
        HttpResponse.json({
          id: 'author-1',
          username: 'alice',
          displayName: 'アリス',
          bio: '',
          createdAt: '2026-07-28T09:00:00Z',
        }),
      ),
      http.get(`${API_BASE}/timeline/public`, () =>
        HttpResponse.json({ items: [post('p1', '公開投稿です', false)], nextCursor: null }),
      ),
    )

    renderPage()

    expect(await screen.findByRole('textbox', { name: '投稿内容' })).toBeInTheDocument()
  })

  it('投稿が無ければその旨を表示する', async () => {
    server.use(
      http.get(`${API_BASE}/users/me`, () => HttpResponse.json({}, { status: 401 })),
      http.post(`${API_BASE}/auth/refresh`, () => HttpResponse.json({}, { status: 401 })),
      http.get(`${API_BASE}/timeline/public`, () =>
        HttpResponse.json({ items: [], nextCursor: null }),
      ),
    )

    renderPage()

    expect(await screen.findByText('まだ投稿がありません。')).toBeInTheDocument()
  })

  /** nextCursor があるときだけ「もっと見る」を出し、押すと続きを取得する。 */
  it('カーソルをたどって次ページを読み込む', async () => {
    tokenStore.set('valid-token')
    server.use(
      http.get(`${API_BASE}/users/me`, () =>
        HttpResponse.json({
          id: 'author-1',
          username: 'alice',
          displayName: 'アリス',
          bio: '',
          createdAt: '2026-07-28T09:00:00Z',
        }),
      ),
      http.get(`${API_BASE}/timeline/public`, ({ request }) => {
        const cursor = new URL(request.url).searchParams.get('cursor')
        return cursor === null
          ? HttpResponse.json({ items: [post('p1', '1ページ目', false)], nextCursor: 'CURSOR1' })
          : HttpResponse.json({ items: [post('p2', '2ページ目', false)], nextCursor: null })
      }),
    )

    renderPage()

    expect(await screen.findByText('1ページ目')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'もっと見る' }))

    expect(await screen.findByText('2ページ目')).toBeInTheDocument()
    // 最終ページでは「もっと見る」が消える
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'もっと見る' })).not.toBeInTheDocument(),
    )
  })

  it('取得に失敗したらエラーを表示する', async () => {
    server.use(
      http.get(`${API_BASE}/users/me`, () => HttpResponse.json({}, { status: 401 })),
      http.post(`${API_BASE}/auth/refresh`, () => HttpResponse.json({}, { status: 401 })),
      http.get(`${API_BASE}/timeline/public`, () =>
        HttpResponse.json(
          {
            type: 'urn:x-app-spring:problem:invalid-cursor',
            title: 'Invalid cursor',
            status: 400,
            detail: 'カーソルの形式が正しくありません。',
          },
          { status: 400 },
        ),
      ),
    )

    renderPage()

    // detail をそのまま見せる（type は分岐用、detail は人間向け）
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'カーソルの形式が正しくありません。',
    )
  })
})
