# CLAUDE.md — x_app_spring 開発規約

X(Twitter) 風メッセージアプリ。Spring Boot の REST API + React SPA。**BFF は置かない**（ブラウザから API を直叩き）。

構築計画の全文は `/Users/nakashimamasaya/.claude/plans/java-spring-boot-x-twitter-java-spring-encapsulated-cerf.md`。

---

## 絶対に守るルール

1. **`openapi.yaml` が API の単一の真実。** エンドポイントやスキーマを変えるときは、必ず `api/openapi.yaml` を先に直す。実装から仕様を書き起こす方向は禁止。変更後は必ず「型再生成 → 生成物をコミット → 契約テスト実行」まで行う（`/spec-first` スキル参照）。生成物 `frontend/src/api/generated/` は**追跡対象**。ignore すると CI の drift 検査が素通りする。
2. **`.env` をコミットしない。** public リポジトリのため、一度 push した秘密情報は履歴から消しても漏洩したものとして扱うしかない。秘密の値は `.env` と環境変数のみ。`application.yaml` には `${JWT_SECRET}` のような参照だけを書き、デフォルト値にも本物を書かない。
3. **Entity を HTTP レスポンスに直接返さない。** 必ず `record` の DTO に詰め替える。`email` / `password_hash` / `deleted_at` は DTO に含めない。
4. **LLM 生成コードは単独コミットにする。** 手書き分と混ぜない（理由は「Git 運用」参照）。
5. **セキュリティ・DB クエリ・ビルド設定はローカル LLM に渡さない。** Claude が手書きする。

---

## バージョン（固定）

新しいものが出ても勝手に上げない。上げるときは ADR に理由を残す。

| 対象 | バージョン |
|---|---|
| Java | 25 (LTS) — Gradle Toolchain で自動プロビジョニング |
| Spring Boot | 4.1.x |
| Gradle | 9.6.x (Kotlin DSL) |
| PostgreSQL | 18 |
| Node | 24 (LTS) — **コンテナ内のみ** |
| React | 19.2.x |
| Vite | 8.1.x |

**ホストの Node は v16.5.0 で Vite 8 の要件（20.19+）を満たさない。** npm 系のコマンドは必ず `docker compose run --rm web ...` 経由で実行すること。ホストで `npm` を直接叩かない。

依存のバージョンは `backend/gradle/libs.versions.toml`（Version Catalog）と `frontend/package.json` に集約する。個別の `build.gradle.kts` に直接バージョンを書かない。

---

## ディレクトリ構成

```
api/openapi.yaml          API 契約（単一の真実）
docs/                     要件・ドメインモデル・ADR
backend/src/main/java/com/example/xapp/
  common/                 例外, ProblemDetail, カーソルページング, 設定
  auth/  user/  post/  timeline/
frontend/src/
  api/generated/          openapi.yaml から生成した型（★ git で追跡する）
  api/                    openapi-fetch クライアント
  features/{auth,post,timeline,user}/
tools/llm/                ローカル LLM 呼び出し CLI とプロンプト
.claude/skills/           作業手順のスキル
```

**パッケージは機能別（package by feature）。** `controller/` `service/` `repository/` を全機能でまとめる層別構成にはしない。理由はローカル LLM に渡すコンテキストを 1 ディレクトリに閉じ込めるため。qwen3:14b のコンテキストは 40K なので、この粒度が生成精度に直結する。

各機能パッケージの中は `XxxController` / `XxxService` / `XxxRepository` / `dto/` / `domain/` で構成する。

---

## 設計上の決めごと

- 主キーは **UUIDv7**。時系列ソート可能で、ID から件数を推測されず、インデックスも断片化しにくい。
- ページングは **カーソル方式**（`?cursor=<id>&limit=20`）。オフセットは投稿追加でズレるため使わない。
- エラーレスポンスは **RFC 9457 Problem Details**（Spring Framework 7 の `ProblemDetail`）で統一。
- 投稿は**論理削除**（`deleted_at`）。クエリでは必ず除外する。
- CORS は `SecurityFilterChain` 内で明示設定。`allowCredentials=true`（Refresh Cookie のため）、オリジンは `CORS_ALLOWED_ORIGINS` から読む。
- Refresh Token は HttpOnly Cookie、パスを `/api/v1/auth` に限定して通常 API リクエストに載せない。ローテーション + 再利用検知を行う。
- 認証系エンドポイントにはレート制限を必ず入れる（ブラウザ直叩きのため）。

---

## コマンド

```bash
# 環境
docker compose up -d
docker compose down -v                        # DB ごと初期化
docker compose logs -f api

# バックエンド
./gradlew :backend:test
./gradlew :backend:compileTestJava
./gradlew :backend:bootRun

# フロントエンド（必ずコンテナ経由）
docker compose run --rm web npm test
docker compose run --rm web npm run typecheck
docker compose run --rm web npm run gen:api   # openapi.yaml → TS 型
docker compose run --rm web npx @redocly/cli lint api/openapi.yaml

# E2E
docker compose --profile e2e run --rm e2e npx playwright test

# シークレット検査（push 前に必ず）
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:latest detect --source=/repo --redact
```

---

## 開発フロー

「仕様 → テスト → 実装 → 検証」の順。機能単位の**縦切りスライス**で 1 つずつ green にする。
全 Entity → 全 Repository → … という横切りの進め方はしない（テストが進捗の指標にならなくなるため）。

実装順: `common` → `auth` → `user` → `post` → `timeline` → フロントエンド

---

## ローカル LLM (Ollama) の使い分け

サーバー: `http://192.168.11.35:11434` / モデル: `qwen3:14b`

| Claude が書く | qwen3 に任せる |
|---|---|
| Security 設定、JWT 発行/検証、フィルタチェーン | record DTO、Entity のフィールド定義 |
| Flyway マイグレーション SQL | Entity ↔ DTO の変換メソッド |
| カーソルページングの共通基盤 | 契約が確定済みの Service CRUD |
| JPQL / ネイティブクエリ | Spring Data のメソッド名クエリ |
| Docker / Gradle / CI 設定 | ケース表からのテストメソッド量産 |
| 例外ハンドリングの設計 | React の表示専用コンポーネント、フォーム |

手順は `/local-codegen` スキル。**修復ループは最大 2 回でエスカレーション**し、それ以上粘らず Claude が引き取る。

---

## Git 運用

### ブランチ

`main` は保護（直 push 禁止・PR 必須・CI 必須）。

| プレフィックス | 用途 |
|---|---|
| `chore/` | 土台整備 |
| `docs/` | 仕様・ADR |
| `test/` | テスト先行追加 |
| `feat/` | 機能実装（縦切り 1 スライス = 1 ブランチ） |
| `fix/` | 不具合修正 |

### コミット

Conventional Commits。scope は機能名（`auth`, `post`, `timeline`, `llm`, `docker`, `api`）。

```
feat(post): 投稿作成エンドポイントを実装
test(timeline): カーソルページングの境界値テストを追加
docs(api): openapi.yaml に PostResponse スキーマを追加
```

区切り方（細かめに）:
1. インタフェース/型の追加
2. テストの追加（この時点では fail）
3. 実装（テストが green になる）
4. リファクタ・整形

**LLM 生成コードは必ず単独コミット**にし、trailer を付ける:

```
feat(post): PostResponse / PostSummary DTO を追加

Generated-by: qwen3:14b (ollama)
Reviewed-by: Claude
```

生成物に問題が出たとき `git revert` で 1 コミットだけ戻せる。手書き分と混ぜると切り分けが効かなくなる。ローカル LLM を実装に使う以上、巻き戻しやすさが最大の保険になる。

### 実行権限

- **commit は Claude が自動で行ってよい。**
- **push と PR 作成は都度ユーザーに確認する。**
- merge は squash ではなく **merge commit**（LLM 生成分の単独コミットを履歴に残すため）。
