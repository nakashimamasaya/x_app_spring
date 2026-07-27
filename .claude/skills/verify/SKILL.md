---
name: verify
description: 変更が実際に動くことを確認する定型手順。環境の起動、テスト実行、E2E、本番相当ビルドの検証に使う。「動作確認して」「ちゃんと動く?」と言われたときに使う。
---

# 動作検証

**ホストに Node が無い**（v16.5.0 で Vite 8 非対応）。npm 系は必ず `docker compose run --rm web ...` 経由。

## レベル 1: 静的チェック（数秒）

```bash
./gradlew :backend:compileTestJava
docker compose run --rm web npm run typecheck
docker compose run --rm web npx @redocly/cli lint api/openapi.yaml
```

## レベル 2: テスト（数分）

```bash
./gradlew :backend:test          # 単体 + Testcontainers 統合
docker compose run --rm web npm test
```

Testcontainers は Docker ソケット経由で PostgreSQL を立てる。
`compose.yaml` の `db` とはポートが衝突しない（Testcontainers はランダムポート）。

失敗したら、まず「テストが正しいか」を疑う。仕様は `api/openapi.yaml` が真実。

## レベル 3: 環境の起動

```bash
docker compose up -d
docker compose ps                # 全て healthy になるまで待つ
docker compose logs -f api       # 起動ログを確認
```

| 確認項目 | コマンド / URL |
|---|---|
| API が生きている | `curl -s localhost:8080/actuator/health` → `{"status":"UP"}` |
| Flyway が流れた | `docker compose logs api \| grep -i flyway` |
| API ドキュメント | http://localhost:8080/swagger-ui.html |
| フロントエンド | http://localhost:5173 |

起動しないときの典型:
- `.env` が無い → `cp .env.example .env` して `JWT_SECRET` を設定
- DB が healthy にならない → `docker compose down -v` でボリュームごと作り直す
- HMR が効かない → macOS の bind mount で inotify が届いていない。
  `vite.config.ts` の `server.watch.usePolling` が true か確認する

## レベル 4: E2E

```bash
docker compose --profile e2e run --rm e2e npx playwright test
```

主要シナリオ: 登録 → ログイン → 投稿 → 別ユーザーがフォロー → タイムラインに出る → いいね

失敗時は `playwright-report/` を開く（`.gitignore` 済み）。

## レベル 4.5: 仕様と生成物のズレ

```bash
docker compose run --rm web npm run gen:api
git diff --stat -- frontend/src/api/generated
```

差分が出たら、`openapi.yaml` を直したあと型を再生成し忘れている。生成物もコミットする。

## レベル 5: 手動確認（BFF なし構成の要所）

ブラウザの DevTools で確認する。ここは自動テストで見落としやすい。

- [ ] **CORS**: `localhost:5173` からの API 呼び出しがプリフライトを含めて通る
- [ ] **Refresh Cookie が通常 API に載っていない**
      （Cookie の Path が `/api/v1/auth` に限定されているか）
- [ ] Access Token が localStorage に保存されて**いない**（メモリ保持のみ）
- [ ] Access Token 失効後、自動でリフレッシュされて操作が継続する
- [ ] ログアウト後、Refresh Cookie が消え、リフレッシュが 401 になる
- [ ] 認証系エンドポイントを連打するとレート制限（429）が返る

## レベル 6: 本番相当ビルド

```bash
docker compose -f compose.prod.yaml up -d --build
```

- nginx がフロントの静的ファイルを配信する
- バックエンドは layered jar の runtime イメージ（非 root ユーザー）
- 開発用の DevTools / Swagger UI が**無効**になっていること

## 報告のしかた

テストが落ちたら落ちたと言う。出力を貼る。
「動くはず」で報告しない。実際に走らせた結果だけを報告する。
