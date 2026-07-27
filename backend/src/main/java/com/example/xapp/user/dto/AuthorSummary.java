package com.example.xapp.user.dto;

import java.util.UUID;

/** 投稿に埋め込む著者の最小情報。api/openapi.yaml の AuthorSummary スキーマと対応する。 */
public record AuthorSummary(UUID id, String username, String displayName) {}
