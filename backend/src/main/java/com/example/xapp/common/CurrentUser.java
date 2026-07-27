package com.example.xapp.common;

import java.util.UUID;

/**
 * 認証済みユーザーの識別情報。
 *
 * <p>コントローラは Spring Security の {@code Authentication} を直接扱わず、この型で受け取る。
 * サービス層がセキュリティの実装詳細に依存しないようにするため。
 *
 * <p>未認証のリクエストでは {@code null} が渡る。認証必須のエンドポイントでは
 * SecurityFilterChain が先に 401 を返すため、そこに到達しない。
 */
public record CurrentUser(UUID id, String username) {}
