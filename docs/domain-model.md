# ドメインモデル

対応する要件は [requirements.md](./requirements.md)。API 契約の正は `api/openapi.yaml`。
このドキュメントは**永続化層とドメイン不変条件**を定義する。

---

## ER 図

```mermaid
erDiagram
    users ||--o{ posts : "投稿する"
    users ||--o{ likes : "いいねする"
    posts ||--o{ likes : "いいねされる"
    users ||--o{ follows : "フォローする"
    users ||--o{ follows : "フォローされる"
    users ||--o{ refresh_tokens : "保持する"

    users {
        uuid    id PK "UUIDv7"
        citext  username UK "3-20文字, 大小無視で一意"
        citext  email UK "最大254文字"
        text    password_hash "BCrypt"
        text    display_name "1-50文字"
        text    bio "0-160文字, 既定は空文字"
        timestamptz created_at
        timestamptz updated_at
    }

    posts {
        uuid    id PK "UUIDv7"
        uuid    author_id FK
        text    body "1-280コードポイント"
        timestamptz created_at
        timestamptz deleted_at "NULL なら生存"
    }

    follows {
        uuid    follower_id PK_FK "フォローする側"
        uuid    followee_id PK_FK "フォローされる側"
        timestamptz created_at
    }

    likes {
        uuid    user_id PK_FK
        uuid    post_id PK_FK
        timestamptz created_at
    }

    refresh_tokens {
        uuid    id PK "UUIDv7"
        uuid    user_id FK
        text    token_hash UK "SHA-256。生トークンは保存しない"
        timestamptz expires_at
        timestamptz revoked_at "NULL なら有効"
        timestamptz created_at
    }
```

---

## テーブル定義

### 共通方針

- **主キーは UUIDv7。** 時系列でソート可能なので B-Tree インデックスが断片化せず、かつ連番と違って ID から総件数を推測されない。カーソルページングのカーソルとしてもそのまま使える。
- 日時はすべて `timestamptz`（UTC 保存）。アプリ側では `java.time.Instant` で扱う。
- 文字列長の制約は DB の `CHECK` とアプリのバリデーションの**両方**に置く。DB 側は最後の砦であり、アプリ側はユーザーに分かりやすいエラーを返すためのもの。

### `users`

| カラム | 型 | 制約 |
|---|---|---|
| `id` | `uuid` | PK |
| `username` | `citext` | UNIQUE NOT NULL, `CHECK (username ~ '^[a-zA-Z0-9_]{3,20}$')` |
| `email` | `citext` | UNIQUE NOT NULL, `CHECK (length(email) <= 254)` |
| `password_hash` | `text` | NOT NULL |
| `display_name` | `text` | NOT NULL, `CHECK (length(display_name) BETWEEN 1 AND 50)` |
| `bio` | `text` | NOT NULL DEFAULT `''`, `CHECK (length(bio) <= 160)` |
| `created_at` | `timestamptz` | NOT NULL DEFAULT `now()` |
| `updated_at` | `timestamptz` | NOT NULL DEFAULT `now()` |

`citext` 拡張を使い、`username` と `email` の一意性を**大文字小文字を区別せず**判定する。
`LOWER()` の関数インデックスでも実現できるが、`citext` の方が検索側の書き漏れによるバグが起きない。

> **JPA 側の注意**: `citext` は Hibernate が知らない型なので、Entity の `@Column` に
> `columnDefinition = "citext"` を明示しないと `ddl-auto=validate` が `varchar` を期待して
> 起動時に落ちる。エラーは
> `wrong column type encountered in column [email] in table [users]; found [citext ...]`。
> この 1 行を書き忘れるとアプリが起動しないので、citext カラムを増やすときは必ず付けること。

> `bio` は `NULL` を許さず空文字を既定にする。`NULL` と `''` の 2 状態があると、
> アプリ側の分岐が増えるだけで意味的な違いがないため。

### `posts`

| カラム | 型 | 制約 |
|---|---|---|
| `id` | `uuid` | PK |
| `author_id` | `uuid` | NOT NULL, FK → `users(id)` ON DELETE CASCADE |
| `body` | `text` | NOT NULL, `CHECK (length(body) BETWEEN 1 AND 280)` |
| `created_at` | `timestamptz` | NOT NULL DEFAULT `now()` |
| `deleted_at` | `timestamptz` | NULL 可 |

**論理削除。** `deleted_at IS NULL` が生存条件。すべてのクエリでこの条件を必ず付ける。

> PostgreSQL の `length()` は文字数（コードポイント数）を数えるため、絵文字も 1 文字として扱われる。
> Java 側は `String.length()` が UTF-16 単位で数えてしまうので、
> `codePointCount()` を使わないと DB とアプリで判定がズレる。

### `follows`

| カラム | 型 | 制約 |
|---|---|---|
| `follower_id` | `uuid` | FK → `users(id)` ON DELETE CASCADE |
| `followee_id` | `uuid` | FK → `users(id)` ON DELETE CASCADE |
| `created_at` | `timestamptz` | NOT NULL DEFAULT `now()` |

- 複合主キー `(follower_id, followee_id)` — 重複フォローを DB レベルで防ぐ
- `CHECK (follower_id <> followee_id)` — **自己フォローを DB レベルで禁止**

### `likes`

| カラム | 型 | 制約 |
|---|---|---|
| `user_id` | `uuid` | FK → `users(id)` ON DELETE CASCADE |
| `post_id` | `uuid` | FK → `posts(id)` ON DELETE CASCADE |
| `created_at` | `timestamptz` | NOT NULL DEFAULT `now()` |

複合主キー `(user_id, post_id)`。自分の投稿へのいいねは許可する（制約を置かない）。

### `refresh_tokens`

| カラム | 型 | 制約 |
|---|---|---|
| `id` | `uuid` | PK |
| `user_id` | `uuid` | NOT NULL, FK → `users(id)` ON DELETE CASCADE |
| `token_hash` | `text` | UNIQUE NOT NULL |
| `expires_at` | `timestamptz` | NOT NULL |
| `revoked_at` | `timestamptz` | NULL 可 |
| `created_at` | `timestamptz` | NOT NULL DEFAULT `now()` |

**生のトークンは保存しない。** SHA-256 ハッシュのみを保存する。
DB が漏洩しても、その値をそのまま Refresh に使えないようにするため。

---

## インデックス

カーソルページングとカウント集計を成立させるために必要なもの。

| インデックス | 目的 |
|---|---|
| `posts (author_id, id DESC) WHERE deleted_at IS NULL` | プロフィールの投稿一覧、ホームタイムライン |
| `posts (id DESC) WHERE deleted_at IS NULL` | 公開タイムライン |
| `follows (follower_id, followee_id)` | 主キー。ホームタイムラインの対象ユーザー抽出 |
| `follows (followee_id)` | フォロワー数・フォロワー一覧 |
| `likes (post_id)` | いいね数の集計 |

部分インデックス（`WHERE deleted_at IS NULL`）にすることで、削除済み投稿がインデックスを太らせない。

---

## 不変条件

実装とテストの両方で守る。カッコ内は違反時の HTTP ステータス。

| # | 不変条件 | 強制する層 |
|---|---|---|
| INV-1 | 自分自身をフォローできない（`400`） | DB CHECK + アプリ |
| INV-2 | 同じ相手を二重にフォローできない（冪等に `204`） | DB 複合PK + アプリ |
| INV-3 | 同じ投稿に二重にいいねできない（冪等に `204`） | DB 複合PK + アプリ |
| INV-4 | 投稿を削除できるのは著者のみ（`403`） | アプリ |
| INV-5 | 削除済み投稿は取得も一覧も不可（`404`） | アプリ（全クエリに `deleted_at IS NULL`） |
| INV-6 | 投稿本文は前後の空白を除去して 1〜280 コードポイント（`400`） | DB CHECK + アプリ |
| INV-7 | `username` / `email` は大小を区別せず一意（`409`） | DB citext UNIQUE + アプリ |
| INV-8 | `email` はいかなるレスポンスにも含めない | アプリ（DTO に定義しない） |
| INV-9 | Refresh Token は使用ごとにローテーションする | アプリ |
| INV-10 | 失効済み Refresh Token の再提示で、そのユーザーの全トークンを失効させる | アプリ |

INV-2 と INV-3 は「冪等」なので、DB の一意制約違反を**エラーとして返さず握りつぶす**点に注意。
`ON CONFLICT DO NOTHING` で実装し、競合状態でも `204` を返す。

---

## カーソルページング

オフセットページングは、閲覧中に新しい投稿が入ると同じ投稿が 2 回出たり飛んだりする。
タイムラインは常に先頭に追加されるため、カーソル方式を採る。

### 方式

主キーが UUIDv7 で時系列ソート可能なので、**カーソルは「最後に返した投稿の id」**でよい。
複合キー（`created_at` + `id`）は不要。

```
GET /timeline/public?limit=20
  → { items: [...20件...], nextCursor: "MDE5..." }

GET /timeline/public?limit=20&cursor=MDE5...
  → 続き
```

### クエリの形

```sql
SELECT ... FROM posts
WHERE deleted_at IS NULL
  AND (:cursor IS NULL OR id < :cursor)
ORDER BY id DESC
LIMIT :limit + 1;   -- 1件多く取り、次ページの有無を判定する
```

`limit + 1` 件取得して、`limit` を超えたら「次がある」と判断し、余分な 1 件を捨てて
その直前の要素の id を `nextCursor` にする。件数を数える `COUNT(*)` は発行しない。

### カーソルの形式

**クライアントから見て不透明な文字列**として定義する。中身は UUID の Base64URL エンコード。

内部表現を隠す理由は、将来 fan-out on write に切り替えたりソートキーを変えたりしたときに、
クライアントを壊さず実装を差し替えられるようにするため。
`api/openapi.yaml` にも「クライアントはこの値を解釈してはならない」と明記する。

不正・復号不能なカーソルは `400`。**次ページが無いときは `nextCursor` を `null` にする**
（空文字にしない。JSON 上で「無い」ことが型で表現できるため）。

---

## カウンタの扱い

いいね数・フォロワー数・投稿数は**都度集計**する。`users` や `posts` に非正規化カラムを持たない。

理由は、非正規化するとカウンタの更新漏れ・二重更新というバグが入り込み、
MVP の想定データ量（ユーザー 1,000 / 投稿 100,000）では集計コストが問題にならないため。

N+1 を避けるため、一覧取得では**投稿一覧と集計を 1 クエリにまとめる**（`LEFT JOIN` + `GROUP BY`、
または相関サブクエリ）。ここは Spring Data のメソッド名クエリでは表現できないので、
JPQL またはネイティブクエリを手書きする領域になる。

性能が問題になった時点で非正規化を検討し、その判断は ADR に残す。
