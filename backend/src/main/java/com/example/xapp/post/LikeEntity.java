package com.example.xapp.post;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * likes テーブル。複合主キー {@code (user_id, post_id)}。
 *
 * <p>いいねは冪等な操作（docs/adr/0005）。自分の投稿へのいいねも許可する。
 */
@Entity
@Table(name = "likes")
public class LikeEntity {

    @EmbeddedId
    private LikeId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LikeEntity() {
        // JPA 用
    }

    public LikeId getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** 複合主キー。JPA の要求により record にできない。 */
    @Embeddable
    public static class LikeId implements Serializable {

        @Column(name = "user_id", nullable = false)
        private UUID userId;

        @Column(name = "post_id", nullable = false)
        private UUID postId;

        protected LikeId() {
            // JPA 用
        }

        public LikeId(UUID userId, UUID postId) {
            this.userId = userId;
            this.postId = postId;
        }

        public UUID getUserId() {
            return userId;
        }

        public UUID getPostId() {
            return postId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof LikeId other
                    && Objects.equals(userId, other.userId)
                    && Objects.equals(postId, other.postId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, postId);
        }
    }
}
