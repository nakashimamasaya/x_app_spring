import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Post } from '../api/types'
import { renderWithProviders } from '../test/renderWithProviders'
import { PostCard } from './PostCard'

const basePost: Post = {
  id: '01912d4e-1a2b-7c3d-8e4f-5a6b7c8d9e0f',
  author: { id: 'author-1', username: 'alice', displayName: 'アリス' },
  body: 'はじめての投稿です。',
  createdAt: '2026-07-28T09:30:00Z',
  likeCount: 3,
  likedByMe: false,
}

describe('PostCard', () => {
  it('本文と著者を表示する', () => {
    renderWithProviders(<PostCard post={basePost} />)

    expect(screen.getByText('はじめての投稿です。')).toBeInTheDocument()
    expect(screen.getByText('アリス')).toBeInTheDocument()
    expect(screen.getByText('@alice')).toBeInTheDocument()
  })

  it('いいね済みなら aria-pressed が true になる', () => {
    renderWithProviders(<PostCard post={{ ...basePost, likedByMe: true }} />)

    expect(screen.getByRole('button', { name: 'いいねを取り消す' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  /**
   * 未認証では likedByMe が null（false ではなく「不明」）。
   * この状態でいいねボタンを出すと、押しても 401 になるだけで操作が破綻する。
   */
  it('likedByMe が null ならいいねボタンを出さない', () => {
    renderWithProviders(<PostCard post={{ ...basePost, likedByMe: null }} />)

    expect(screen.queryByRole('button', { name: /いいね/ })).not.toBeInTheDocument()
    // 件数自体は表示する
    expect(screen.getByText(/♥ 3/)).toBeInTheDocument()
  })

  it('いいねボタンを押すとコールバックが呼ばれる', async () => {
    const onToggleLike = vi.fn()
    renderWithProviders(<PostCard post={basePost} onToggleLike={onToggleLike} />)

    await userEvent.click(screen.getByRole('button', { name: 'いいねする' }))

    expect(onToggleLike).toHaveBeenCalledWith(basePost)
  })

  it('canDelete が false なら削除ボタンを出さない', () => {
    renderWithProviders(<PostCard post={basePost} canDelete={false} />)

    expect(screen.queryByRole('button', { name: '削除' })).not.toBeInTheDocument()
  })

  it('canDelete が true なら削除ボタンを出す', () => {
    renderWithProviders(<PostCard post={basePost} canDelete />)

    expect(screen.getByRole('button', { name: '削除' })).toBeInTheDocument()
  })
})
