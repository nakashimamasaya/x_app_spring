package com.example.xapp.post.dto;

import com.example.xapp.user.dto.AuthorSummary;
import java.time.Instant;
import java.util.UUID;

/**
 * api/openapi.yaml の Post スキーマと対応する。
 *
 * @param likedByMe 閲覧者がいいね済みか。<strong>未認証の場合は {@code null}</strong>
 *     （{@code false} ではなく「不明」を表すため Boolean にしている）
 */
public record PostResponse(
        UUID id,
        AuthorSummary author,
        String body,
        Instant createdAt,
        long likeCount,
        Boolean likedByMe) {}
