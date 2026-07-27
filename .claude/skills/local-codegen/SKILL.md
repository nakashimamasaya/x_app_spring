---
name: local-codegen
description: 定型コードの生成をローカル LLM (Ollama / qwen3:14b) に委譲する。DTO・Entity・変換メソッド・CRUD サービス・テストメソッド量産・React 表示コンポーネントを書くときに使う。セキュリティ設定・JWT・DB クエリ・ビルド設定には使わない。
---

# ローカル LLM への委譲

サーバー `http://192.168.11.35:11434` の `qwen3:14b` に定型コードを書かせ、Claude がレビューする。
目的はトークン節約ではなく**役割分担**。Claude は契約と設計に集中し、機械的な埋め込みを外に出す。

## 委譲してよいか判断する

| 委譲する | Claude が手書きする |
|---|---|
| record DTO | Spring Security 設定、`SecurityFilterChain` |
| Entity のフィールド定義とアノテーション | JWT の発行・検証・ローテーション |
| Entity ↔ DTO の変換メソッド | Flyway マイグレーション SQL |
| 契約が確定済みの Service の CRUD | JPQL / `@Query` / ネイティブ SQL |
| Spring Data のメソッド名クエリ | カーソルページングの共通基盤 |
| ケース表からのテストメソッド量産 | 例外ハンドリングの設計、`@ControllerAdvice` |
| React の表示専用コンポーネント、フォーム | Gradle / Docker / CI 設定 |

**迷ったら委譲しない。** 右列は 14B の量子化モデルが最も間違えやすく、かつ間違いのコストが最も高い領域。

前提として、委譲するには**契約が既に確定している**必要がある。
`openapi.yaml` のスキーマ、メソッドシグネチャ、受け入れテストのいずれかが未確定なら、まずそれを Claude が固める。

## 手順

### 1. タスクカードを書く

`tools/llm/tasks/<name>.md` に置く（このディレクトリは `.gitignore` 済み）。
**1 カード = 1 ファイル。自己完結。** qwen3 は曖昧な指示で大きく外す。

```markdown
# 生成対象
backend/src/main/java/com/example/xapp/post/dto/PostResponse.java

# 制約
- package com.example.xapp.post.dto;
- Java 25 record
- 使ってよい import: java.time.Instant, java.util.UUID, com.example.xapp.user.dto.AuthorSummary
- Lombok は使わない
- コメントは書かない

# 仕様
（openapi.yaml の該当スキーマをそのまま貼る）

# 受け入れ条件
- backend/src/test/java/.../PostResponseTest.java が通ること
```

**「使ってよい import」は必ず列挙する。** これが無いと存在しないクラスを import して落ちる。

参照する既存コードがある場合は、パスを書くのではなく**中身を抜粋して貼る**。
qwen3 にはファイルを読む手段がない。

### 2. 生成する

```bash
python3 tools/llm/gen.py \
  --task tools/llm/tasks/post-dto.md \
  --prompt java-record \
  --out backend/src/main/java/com/example/xapp/post/dto/PostResponse.java
```

`--prompt` の選択肢:

| 値 | 用途 |
|---|---|
| `java-record` | DTO、値オブジェクト |
| `java-service` | Service の実装 |
| `java-test` | テストクラス |
| `react-component` | React コンポーネント |

`--dry-run` でファイルに書かず標準出力に出せる。初回は dry-run で確認するのが安全。

### 3. 検証する

```bash
./gradlew :backend:compileJava
./gradlew :backend:test --tests '*PostResponseTest'
# フロントエンド
docker compose run --rm web npm run typecheck
```

### 4. 失敗したら差し戻す（最大 2 回）

```bash
./gradlew :backend:compileJava 2>&1 | tail -40 > /tmp/err.txt
python3 tools/llm/gen.py \
  --task tools/llm/tasks/post-dto.md \
  --prompt java-record \
  --out backend/src/main/java/com/example/xapp/post/dto/PostResponse.java \
  --repair /tmp/err.txt
```

`--repair` は `--out` の既存内容を「前回の出力」として会話に含め、エラーとともに差し戻す。

> **2 回で直らなければ Claude が引き取って手書きする。ここで粘らない。**
> ローカル LLM の修復ループは容易に無限ループ化し、最初から全部手で書くより遅くなる。
> 2 回失敗したということは、タスクカードの分解が粗すぎるか、そもそも委譲すべきでない領域だったということ。

### 5. 必ずレビューする

コンパイルが通っても、Claude が `git diff` を読んでから次に進む。実際に観測された逸脱:

- **タスクカードのスタイル指示を無視する。** 「コンパクトコンストラクタで」と書いても
  明示的な canonical constructor を書いてくることがある（コンパイルは通る）。
- 指示していない `null` チェックやデフォルト値を勝手に足す。
- フィールド名を snake_case に変換してしまう（契約は camelCase）。
- 説明文が本文に混ざる（`gen.py` の `[warn]` が検出するが、見逃す場合もある）。

特に次は毎回確認する:
- `email` / `passwordHash` / `deletedAt` がレスポンス DTO に混入していないか
- フィールド名が `openapi.yaml` と完全一致しているか
- 許可していない import が増えていないか

### 6. 単独コミットにする

```bash
git add <生成されたファイル>
git commit -m "$(cat <<'EOF'
feat(post): PostResponse DTO を追加

Generated-by: qwen3:14b (ollama)
Reviewed-by: Claude
EOF
)"
```

手書き分と同じコミットに混ぜない。品質問題が出たとき `git revert` で 1 コミットだけ戻せる状態を保つ。

## トラブルシューティング

| 症状 | 対処 |
|---|---|
| 接続できない | Ollama サーバーが同一ネットワークにあるか確認。`curl -s $OLLAMA_BASE_URL/api/tags` |
| タイムアウト | `--timeout 600`。それでも切れるならタスクカードを分割する |
| 出力が途中で切れる | タスクが大きすぎる。1 ファイルを複数カードに割る |
| 説明文が混ざる | `--prompt` の指定が間違っていないか確認。system prompt が効いていない |
| 毎回違う結果になる | `temperature=0.1` 固定なので大きくは振れないはず。タスクカードが曖昧 |
