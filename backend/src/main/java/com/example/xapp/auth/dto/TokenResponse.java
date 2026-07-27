package com.example.xapp.auth.dto;

/**
 * Refresh Token はここに含まれない。HttpOnly Cookie で別途配られる。
 *
 * @param accessToken クライアントは<strong>メモリにのみ保持</strong>すること。
 *     localStorage に保存してはならない
 * @param expiresIn Access Token の有効秒数
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static final String BEARER = "Bearer";

    public static TokenResponse bearer(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, BEARER, expiresIn);
    }
}
