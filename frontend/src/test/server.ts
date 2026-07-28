import { setupServer } from 'msw/node'

/**
 * MSW のサーバー。ハンドラは各テストで `server.use(...)` して足す。
 *
 * レスポンスの形は api/openapi.yaml の examples に合わせること。
 * ここが仕様とズレると、テストは通るのに実際は動かないという最悪の状態になる。
 */
export const server = setupServer()

export const API_BASE = 'http://localhost:8080/api/v1'
