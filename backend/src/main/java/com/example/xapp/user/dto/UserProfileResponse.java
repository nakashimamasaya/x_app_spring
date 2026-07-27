package com.example.xapp.user.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * プロフィール表示用。api/openapi.yaml の UserProfile スキーマと対応する。
 *
 * <p>OpenAPI 側は User との allOf 合成だが、JSON の形は同じなので record では平坦に持つ。
 *
 * @param isFollowing 閲覧者がフォロー中か。<strong>未認証の場合は {@code null}</strong>
 *     （{@code false} ではなく「不明」を表すため Boolean にしている）
 */
public record UserProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        Instant createdAt,
        long postCount,
        long followerCount,
        long followingCount,
        Boolean isFollowing) {}
