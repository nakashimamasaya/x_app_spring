package com.example.xapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.xapp.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Flyway のマイグレーションが実際の PostgreSQL に適用でき、
 * docs/domain-model.md の不変条件が DB レベルで強制されることを確認する。
 *
 * <p>これはアプリのロジックではなくスキーマそのもののテストなので、
 * 実装がまだ無いフェーズ2 の時点でも green になる。
 */
class SchemaMigrationTest extends AbstractIntegrationTest {

    @Test
    void 全テーブルが作成されている() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables)
                .contains("users", "posts", "follows", "likes", "refresh_tokens");
    }

    @Test
    void 部分インデックスが削除済み投稿を除外している() {
        List<String> defs = jdbcTemplate.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE tablename = 'posts'", String.class);

        assertThat(defs)
                .filteredOn(d -> d.contains("deleted_at IS NULL"))
                .hasSize(2);
    }

    @Test
    void 自分自身をフォローする行は挿入できない() {
        UUID user = insertUser("selffollow", "selffollow@example.com");

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", user, user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 同じ相手を二重にフォローする行は挿入できない() {
        UUID alice = insertUser("dupfollow_a", "dupfollow_a@example.com");
        UUID bob = insertUser("dupfollow_b", "dupfollow_b@example.com");
        jdbcTemplate.update("INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", alice, bob);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO follows (follower_id, followee_id) VALUES (?, ?)", alice, bob))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void username_は大文字小文字を区別せず一意である() {
        insertUser("CaseTest", "casetest@example.com");

        assertThatThrownBy(() -> insertUser("casetest", "casetest2@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 投稿本文は281文字以上を挿入できない() {
        UUID author = insertUser("longbody", "longbody@example.com");
        String tooLong = "a".repeat(281);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO posts (id, author_id, body) VALUES (?, ?, ?)",
                        UUID.randomUUID(), author, tooLong))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 投稿本文の長さはコードポイント数で数えられる() {
        UUID author = insertUser("emoji", "emoji@example.com");
        // サロゲートペアの絵文字 280 個。Java の String.length() では 560 になるが、
        // PostgreSQL の length() は 280 と数えるので挿入できる
        String emoji = "😀".repeat(280);
        assertThat(emoji.length()).isEqualTo(560);

        int inserted = jdbcTemplate.update(
                "INSERT INTO posts (id, author_id, body) VALUES (?, ?, ?)",
                UUID.randomUUID(), author, emoji);

        assertThat(inserted).isEqualTo(1);
    }

    private UUID insertUser(String username, String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, password_hash, display_name) VALUES (?, ?, ?, ?, ?)",
                id, username, email, "dummy-hash", "テスト");
        return id;
    }
}
