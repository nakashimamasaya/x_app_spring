import { beforeEach, describe, expect, it, vi } from 'vitest'
import { tokenStore } from './tokenStore'

describe('tokenStore', () => {
  beforeEach(() => {
    tokenStore.clear()
    localStorage.clear()
    sessionStorage.clear()
  })

  it('設定したトークンを取得できる', () => {
    tokenStore.set('token-abc')

    expect(tokenStore.get()).toBe('token-abc')
  })

  /**
   * BFF を挟まない構成では XSS がそのままトークン奪取になる（docs/adr/0001）。
   * ストレージに書かないことは要件なので、実装が変わったら落ちるようにしておく。
   */
  it('localStorage と sessionStorage には一切書き込まない', () => {
    tokenStore.set('secret-token')

    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    expect(JSON.stringify(localStorage)).not.toContain('secret-token')
  })

  it('clear で null になる', () => {
    tokenStore.set('token-abc')

    tokenStore.clear()

    expect(tokenStore.get()).toBeNull()
  })

  it('購読者に変更が通知される', () => {
    const listener = vi.fn()
    tokenStore.subscribe(listener)

    tokenStore.set('token-abc')

    expect(listener).toHaveBeenCalledWith('token-abc')
  })

  it('購読を解除すると通知されない', () => {
    const listener = vi.fn()
    const unsubscribe = tokenStore.subscribe(listener)
    unsubscribe()

    tokenStore.set('token-abc')

    expect(listener).not.toHaveBeenCalled()
  })
})
