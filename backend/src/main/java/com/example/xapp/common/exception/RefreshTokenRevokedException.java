package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/** Refresh Token が無効・期限切れ・失効済み（INV-10 の盗難検知を含む）。 */
public class RefreshTokenRevokedException extends AppException {

    public RefreshTokenRevokedException() {
        super(HttpStatus.UNAUTHORIZED, "urn:x-app-spring:problem:refresh-token-revoked", "Refresh token revoked", "セッションが無効化されました。再度ログインしてください。");
    }
}
