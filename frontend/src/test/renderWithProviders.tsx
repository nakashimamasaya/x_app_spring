import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { MemoryRouter } from 'react-router'

/**
 * テスト用のプロバイダ。
 *
 * リトライを切っているのは、エラー系のテストで実際の待ち時間が発生するのを防ぐため。
 * 本番の設定（retry: 1）とは意図的に変えている。
 */
export function renderWithProviders(ui: ReactElement, { route = '/' } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  })

  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
    </QueryClientProvider>
  )

  return { ...render(ui, { wrapper: Wrapper }), queryClient }
}
