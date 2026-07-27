package com.example.xapp.user.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * ユーザーの公開情報。api/openapi.yaml の User スキーマと対応する。
 *
 * <p><strong>email を追加してはならない</strong>（docs/adr/0004）。
 * 自分自身のプロフィールであっても返さない。
 */
public record UserResponse(
        UUID id, String username, String displayName, String bio, Instant createdAt) {}
