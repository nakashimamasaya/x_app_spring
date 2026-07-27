# x_app_spring

X(Twitter) 風のメッセージアプリ。Java + Spring Boot の REST API と React SPA で構成する学習・実験用プロジェクト。

BFF を挟まず、ブラウザから API を直接呼び出す構成を採る。

## 技術スタック

| 領域 | 採用 |
|---|---|
| 言語 / ランタイム | Java 25 (LTS) |
| フレームワーク | Spring Boot 4.1 (Spring Framework 7 / Jakarta EE 11) |
| ビルド | Gradle 9.6 (Kotlin DSL) |
| DB | PostgreSQL 18 + Flyway |
| 認証 | JWT (Access + Refresh) / Spring Security 7 |
| フロントエンド | React 19.2 + Vite 8 + TypeScript |
| 状態管理 | TanStack Query v5 |
| スタイル | Tailwind CSS v4 |
| API 契約 | OpenAPI 3.1（スペックファースト） |
| テスト | JUnit 5 / Testcontainers / Vitest / Playwright |
| コンテナ | Docker Compose |

## 前提

- Docker / Docker Compose
- JDK 17〜26 のいずれか（Gradle Toolchain が JDK 25 を自動取得する）
- Node.js は**不要**。フロントエンドは全てコンテナ内で動かす。

## セットアップ

```bash
cp .env.example .env
# .env の JWT_SECRET を openssl rand -base64 48 の出力に差し替える
docker compose up -d
```

| URL | 内容 |
|---|---|
| http://localhost:5173 | フロントエンド |
| http://localhost:8080/api/v1 | API |
| http://localhost:8080/swagger-ui.html | API ドキュメント |

## よく使うコマンド

```bash
docker compose up -d                          # 開発環境の起動
docker compose logs -f api                    # API のログ
./gradlew :backend:test                       # バックエンドのテスト
docker compose run --rm web npm test          # フロントエンドのテスト
docker compose run --rm web npm run gen:api   # openapi.yaml から TS 型を再生成
docker compose --profile e2e run --rm e2e npx playwright test
```

## ドキュメント

| ファイル | 内容 |
|---|---|
| `CLAUDE.md` | 開発規約（AI エージェント向けだが人間にも有効） |
| `api/openapi.yaml` | API 契約。**単一の真実** |
| `docs/requirements.md` | 機能要件 |
| `docs/domain-model.md` | ドメインモデル |
| `docs/adr/` | 設計判断の記録 |

## 開発の進め方

「仕様策定 → テスト作成 → 実装 → 動作検証」の順で、機能単位の縦切りスライスごとに進める。
定型コードの生成にはローカル LLM (Ollama / qwen3:14b) を併用する。詳細は `CLAUDE.md` を参照。
