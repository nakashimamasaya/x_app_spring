import type { components } from './generated/schema'

/**
 * 生成された型への別名。画面側が `components['schemas'][...]` を直接書くと、
 * 生成物の構造にコードが縛られるため、ここで一段挟む。
 */
export type User = components['schemas']['User']
export type UserProfile = components['schemas']['UserProfile']
export type Post = components['schemas']['Post']
export type PostPage = components['schemas']['PostPage']
export type UserPage = components['schemas']['UserPage']
export type Problem = components['schemas']['Problem']

/**
 * エラーレスポンスから表示用のメッセージを取り出す。
 *
 * `type` が機械可読な識別子で、`detail` は人間向け（api/openapi.yaml）。
 * 分岐は `type` で行い、表示には `detail` を使う。
 */
export function problemMessage(error: unknown, fallback = 'エラーが発生しました。'): string {
  if (typeof error === 'object' && error !== null && 'detail' in error) {
    const detail = (error as Problem).detail
    if (typeof detail === 'string' && detail.length > 0) {
      return detail
    }
  }
  return fallback
}

/** バリデーションエラーの errors 配列（api/openapi.yaml の ValidationProblem）。 */
export function fieldErrors(error: unknown): { field: string; message: string }[] {
  if (typeof error === 'object' && error !== null && 'errors' in error) {
    const errors = (error as { errors?: unknown }).errors
    if (Array.isArray(errors)) {
      return errors as { field: string; message: string }[]
    }
  }
  return []
}
