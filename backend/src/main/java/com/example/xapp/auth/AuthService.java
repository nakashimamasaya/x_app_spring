package com.example.xapp.auth;

import com.example.xapp.auth.dto.LoginRequest;
import com.example.xapp.auth.dto.RegisterRequest;
import com.example.xapp.auth.dto.TokenResponse;
import com.example.xapp.user.dto.UserResponse;

/**
 * 認証まわりのユースケース。実装はフェーズ3。
 *
 * <p>Refresh Token の生値はコントローラと Cookie の間だけで扱い、
 * 永続化層にはハッシュしか渡さない。
 */
public interface AuthService {

    UserResponse register(RegisterRequest request);

    /**
     * @return 発行された Access Token と、Cookie に載せる Refresh Token の生値
     */
    AuthResult login(LoginRequest request);

    /**
     * Refresh Token をローテーションして新しい Access Token を発行する。
     *
     * <p>失効済みトークンが提示された場合は盗難とみなし、そのユーザーの
     * 全 Refresh Token を失効させたうえで例外を投げる（INV-10）。
     */
    AuthResult refresh(String refreshTokenValue);

    /** 冪等。トークンが無効・不在でも例外を投げない。 */
    void logout(String refreshTokenValue);

    /** Access Token と、Cookie に設定する Refresh Token の生値をまとめて返す。 */
    record AuthResult(TokenResponse tokens, String refreshTokenValue) {}
}
