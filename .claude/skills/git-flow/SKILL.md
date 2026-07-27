---
name: git-flow
description: このリポジトリのブランチ作成・コミット分割・PR 作成の手順。作業を始めるとき、コミットするとき、PR を出すときに使う。
---

# Git 運用

`main` は保護されている（直 push 禁止・PR 必須・CI 必須）。

## 実行権限

- **commit は Claude が自動で行ってよい。**
- **push と PR 作成は都度ユーザーに確認する。** 勝手に push しない。
- public リポジトリのため、push する前に必ずシークレット検査を通す。

## 作業開始

```bash
git switch main && git pull
git switch -c feat/post
```

| プレフィックス | 用途 | 例 |
|---|---|---|
| `chore/` | 土台整備 | `chore/phase0-scaffold` |
| `docs/` | 仕様・ADR | `docs/phase1-openapi-spec` |
| `test/` | テスト先行追加 | `test/phase2-integration-tests` |
| `feat/` | 機能実装 | `feat/auth`, `feat/post` |
| `fix/` | 不具合修正 | `fix/cursor-pagination-boundary` |

**フェーズ 3 では縦切りスライスとブランチを 1:1 で対応させる。**
`feat/auth` `feat/user` `feat/post` `feat/timeline` のように。
これでレビュー単位・巻き戻し単位・テストが green になる単位が全て揃う。

## コミット

Conventional Commits。scope は機能名（`auth`, `post`, `timeline`, `api`, `llm`, `docker`, `ci`）。

```
feat(post): 投稿作成エンドポイントを実装
fix(timeline): カーソルが最終ページで無限ループする問題を修正
test(user): フォロー関連の境界値テストを追加
docs(api): openapi.yaml に PostResponse スキーマを追加
chore(docker): compose.yaml に e2e プロファイルを追加
refactor(common): カーソルのエンコード処理を共通化
```

**区切りは細かめに。** 1 スライスの中でも次の順にコミットを分ける:

1. インタフェース/型の追加（`feat(post): 投稿 API のインタフェースを定義`）
2. テストの追加（`test(post): 投稿作成の統合テストを追加` — この時点では fail）
3. 実装（`feat(post): 投稿作成を実装` — テストが green になる）
4. リファクタ・整形

### LLM 生成コードは必ず単独コミット

```bash
git add backend/src/main/java/com/example/xapp/post/dto/PostResponse.java
git commit -m "$(cat <<'EOF'
feat(post): PostResponse DTO を追加

Generated-by: qwen3:14b (ollama)
Reviewed-by: Claude
EOF
)"
```

手書き分と混ぜない。生成物の品質に問題が出たとき `git revert` で 1 コミットだけ戻せる。
ローカル LLM を実装に使う以上、**巻き戻しやすさが最大の保険**になる。

## push 前の必須チェック

public リポジトリなので、一度 push した秘密情報は履歴から消しても漏洩したものとして扱うしかない。

```bash
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:latest detect --source=/repo --redact
git status --short          # .env が出ていないこと
git diff --cached --stat    # 意図しないファイルが混ざっていないこと
```

0 leaks を確認してからユーザーに push の可否を確認する。

## PR

```bash
gh pr create --fill --base main
```

本文には必ず含める（PR テンプレートで定型化済み）:
- 対応フェーズとスライス
- テスト結果（`./gradlew :backend:test` の要約）
- qwen3 生成分の有無と、レビューで直した点

merge は **squash ではなく merge commit**。LLM 生成分の単独コミットを履歴に残すため。

```bash
gh pr merge --merge
```

## やらないこと

- `main` への直 push
- `git push --force`（共有ブランチ）
- `.env` のコミット — `.gitignore` の先頭で防いでいるが、`git add -f` で強制追加しない
- 生成物（`frontend/src/api/generated/`）を**コミットから外す**こと — これは追跡対象。
  外すと CI の `contract-drift` 検査が素通りする
