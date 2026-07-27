package com.example.xapp.user;

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
 * follows テーブル。複合主キー {@code (follower_id, followee_id)}。
 *
 * <p>フォローは冪等な操作なので（docs/adr/0005）、挿入は
 * {@code INSERT ... ON CONFLICT DO NOTHING} で行い、複合主キー違反を
 * エラーにせず握りつぶす。競合状態でも 204 を返せるようにするため。
 */
@Entity
@Table(name = "follows")
public class FollowEntity {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FollowEntity() {
        // JPA 用
    }

    public FollowId getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** 複合主キー。JPA の要求により可変クラス + 引数なしコンストラクタが必要で record にできない。 */
    @Embeddable
    public static class FollowId implements Serializable {

        @Column(name = "follower_id", nullable = false)
        private UUID followerId;

        @Column(name = "followee_id", nullable = false)
        private UUID followeeId;

        protected FollowId() {
            // JPA 用
        }

        public FollowId(UUID followerId, UUID followeeId) {
            this.followerId = followerId;
            this.followeeId = followeeId;
        }

        public UUID getFollowerId() {
            return followerId;
        }

        public UUID getFolloweeId() {
            return followeeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof FollowId other
                    && Objects.equals(followerId, other.followerId)
                    && Objects.equals(followeeId, other.followeeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(followerId, followeeId);
        }
    }
}
