package com.example.xapp.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * users テーブル。定義の正は backend/src/main/resources/db/migration/V1__init.sql。
 *
 * <p>ddl-auto は validate 固定なので、このクラスとマイグレーションがズレると
 * コンテキスト起動時に落ちる。
 *
 * <p>ID はアプリ側で UUIDv7 を採番する（docs/adr/0002）。DB の DEFAULT には頼らない。
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * citext カラム。大文字小文字を区別せず一意（INV-7）。
     *
     * <p>{@code columnDefinition} の指定が必須。citext は Hibernate が知らない型で、
     * 省略すると ddl-auto=validate が varchar を期待して起動時に落ちる。
     */
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String username;

    /**
     * どのレスポンス DTO にも含めない（docs/adr/0004）。
     * 保持しているのは将来のパスワードリセットのため。
     */
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** NULL は許さず空文字を既定とする。NULL と "" の 2 状態を作らないため。 */
    @Column(nullable = false)
    private String bio;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
        // JPA 用
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
