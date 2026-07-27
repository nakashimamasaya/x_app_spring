---
name: spec-first
description: api/openapi.yaml を変更するときの手順と波及チェックリスト。エンドポイント追加・スキーマ変更・エラーレスポンス変更のときに必ず使う。
---

# OpenAPI スペックファースト

`api/openapi.yaml` が API の**単一の真実**。実装から仕様を書き起こす方向は禁止。

## 変更の順序（この順を崩さない）

1. `api/openapi.yaml` を編集する
2. lint を通す
3. TypeScript 型を再生成する
4. バックエンドの契約テストを更新する
5. 実装を追従させる
6. 契約テストが green になることを確認する

実装を先に変えると、仕様と実装が食い違ったまま気づかず進む。CI の `contract-drift` ジョブが
検出するが、そこで気づくのは遅い。

## コマンド

```bash
# 2. lint（OpenAPI 3.1 として妥当か）
docker compose run --rm web npx @redocly/cli lint api/openapi.yaml

# 3. TypeScript 型の再生成（ホストに Node が無いので必ずコンテナ経由）
docker compose run --rm web npm run gen:api

# 4-6. 契約テスト
./gradlew :backend:test --tests '*ContractTest'
```

## 波及チェックリスト

エンドポイントやスキーマを変えたら、以下を全て確認する。

- [ ] `api/openapi.yaml` の `examples` を更新した
      （E2E のテストデータと MSW のモックがこれを元にしている）
- [ ] `docker compose run --rm web npm run gen:api` を実行した
- [ ] `frontend/src/api/generated/` の diff を確認した（追跡外だが目視する）
- [ ] バックエンドの DTO のフィールド名がスキーマと**完全一致**している（camelCase のまま）
- [ ] 統合テストに新しいエンドポイントのケースを足した（正常系・認可エラー・バリデーションエラー）
- [ ] エラーレスポンスが RFC 9457 Problem Details の形になっている
- [ ] 破壊的変更なら `docs/adr/` に理由を記録した
- [ ] フロントエンドの呼び出し箇所を追従させた（`npm run typecheck` で検出できる）

## スキーマを書くときの規約

- プロパティ名は **camelCase**。snake_case にしない。
- 日時は `type: string, format: date-time`（ISO 8601 / UTC）。
- ID は `type: string, format: uuid`。
- ページングのレスポンスは必ず `{ items: [...], nextCursor: string | null }` の形にする。
- 全てのスキーマに `examples` を付ける。これを省くとテストデータを別途作る羽目になる。
- `required` を明示する。省略可能なフィールドは `nullable: true` ではなく
  `required` から外す（OpenAPI 3.1 では `type: [string, "null"]` を使う）。
- **レスポンススキーマに `email` / `passwordHash` / `deletedAt` を定義しない。**
  仕様の時点で入れなければ、実装で漏れることもない。

## エラーレスポンスの共通形

```yaml
components:
  schemas:
    Problem:
      type: object
      properties:
        type: { type: string, format: uri }
        title: { type: string }
        status: { type: integer }
        detail: { type: string }
        instance: { type: string, format: uri }
```

各エンドポイントで `400` / `401` / `403` / `404` / `409` / `429` のうち
**起こりうるものだけ**を列挙する。「念のため全部書く」はしない。
