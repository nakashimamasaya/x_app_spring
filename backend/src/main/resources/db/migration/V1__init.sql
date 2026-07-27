-- 初期スキーマ。定義の根拠は docs/domain-model.md。
--
-- 方針:
--   * 主キーは UUIDv7（docs/adr/0002）。アプリ側で採番する
--   * 文字列長の制約は DB とアプリの両方に置く。DB 側は最後の砦
--   * 不変条件 INV-1〜3 は CHECK と複合主キーで DB レベルでも強制する

-- username / email を大文字小文字を区別せず一意にする。
-- LOWER() の関数インデックスでも実現できるが、citext なら検索側の
-- 書き漏れによるバグが起きない。
CREATE EXTENSION IF NOT EXISTS citext;

-- ============================================================
CREATE TABLE users (
    id            uuid        PRIMARY KEY,
    username      citext      NOT NULL UNIQUE,
    email         citext      NOT NULL UNIQUE,
    password_hash text        NOT NULL,
    display_name  text        NOT NULL,
    bio           text        NOT NULL DEFAULT '',
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT users_username_format  CHECK (username ~ '^[a-zA-Z0-9_]{3,20}$'),
    CONSTRAINT users_email_length     CHECK (length(email) <= 254),
    CONSTRAINT users_display_name_len CHECK (length(display_name) BETWEEN 1 AND 50),
    -- bio は NULL を許さず空文字を既定にする。NULL と '' の 2 状態があると
    -- 意味的な違いが無いままアプリ側の分岐が増えるため。
    CONSTRAINT users_bio_len          CHECK (length(bio) <= 160)
);

-- ============================================================
CREATE TABLE posts (
    id         uuid        PRIMARY KEY,
    author_id  uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body       text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    -- 論理削除。deleted_at IS NULL が生存条件（INV-5）
    deleted_at timestamptz,

    -- PostgreSQL の length() は文字数（コードポイント数）を数えるので絵文字は 1 文字。
    -- Java 側は String.length() が UTF-16 単位で数えるため codePointCount() が必要（INV-6）
    CONSTRAINT posts_body_len CHECK (length(body) BETWEEN 1 AND 280)
);

-- 部分インデックスにして、削除済み投稿がインデックスを太らせないようにする
CREATE INDEX posts_author_id_idx ON posts (author_id, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX posts_id_idx        ON posts (id DESC)            WHERE deleted_at IS NULL;

-- ============================================================
CREATE TABLE follows (
    follower_id uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  timestamptz NOT NULL DEFAULT now(),

    -- 複合主キーが二重フォローを防ぐ（INV-2）
    PRIMARY KEY (follower_id, followee_id),
    -- 自己フォローの禁止（INV-1）
    CONSTRAINT follows_no_self CHECK (follower_id <> followee_id)
);

CREATE INDEX follows_followee_id_idx ON follows (followee_id);

-- ============================================================
CREATE TABLE likes (
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    post_id    uuid        NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),

    -- 複合主キーが二重いいねを防ぐ（INV-3）。
    -- 自分の投稿へのいいねは許可するので、その制約は置かない
    PRIMARY KEY (user_id, post_id)
);

CREATE INDEX likes_post_id_idx ON likes (post_id);

-- ============================================================
CREATE TABLE refresh_tokens (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- 生のトークンは保存しない。SHA-256 ハッシュのみ。
    -- DB が漏洩してもその値をそのまま Refresh に使えないようにするため
    token_hash text        NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    -- NULL なら有効。ローテーション時と盗難検知時に埋める（INV-9, INV-10）
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
