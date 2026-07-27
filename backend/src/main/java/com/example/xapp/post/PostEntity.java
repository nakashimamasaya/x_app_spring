package com.example.xapp.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * posts テーブル。論理削除で、{@code deletedAt == null} が生存条件（INV-5）。
 *
 * <p>著者は {@code authorId} を素の UUID で持ち、{@code @ManyToOne} にしない。
 * 一覧取得では投稿・著者・いいね数を 1 クエリでまとめて取るため（docs/adr/0006）、
 * 関連を張ると遅延ロードによる N+1 を招きやすい。
 */
@Entity
@Table(name = "posts")
public class PostEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    /** 1〜280 コードポイント。Java 側は codePointCount() で数えること（INV-6）。 */
    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** NULL なら生存。全クエリで {@code deleted_at IS NULL} を必ず付ける。 */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected PostEntity() {
        // JPA 用
    }

    public UUID getId() {
        return id;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
