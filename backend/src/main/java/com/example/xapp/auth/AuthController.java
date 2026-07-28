package com.example.xapp.auth;

import com.example.xapp.auth.dto.LoginRequest;
import com.example.xapp.auth.dto.RegisterRequest;
import com.example.xapp.auth.dto.TokenResponse;
import com.example.xapp.common.AppProperties;
import com.example.xapp.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * api/openapi.yaml の auth タグに対応する。
 *
 * <p>Refresh Token は HttpOnly Cookie で扱う。名前とパスは契約で決まっているので変えない。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** 契約（openapi.yaml の Set-Cookie 例）と一致させること。 */
    public static final String REFRESH_COOKIE = "refresh_token";

    /**
     * Cookie のパスを /auth 配下に限定する。
     *
     * <p>これにより通常の API リクエストに Refresh Cookie が載らない。BFF を挟まない構成で
     * Cookie の露出を最小化するための要。context-path (/api/v1) を含めた絶対パスで指定する。
     */
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final AppProperties props;

    public AuthController(AuthService authService, AppProperties props) {
        this.authService = authService;
        this.props = props;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withRefreshCookie(authService.refresh(refreshToken));
    }

    /** 冪等。Cookie が無くても 204 を返す。 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<TokenResponse> withRefreshCookie(AuthService.AuthResult result) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshTokenValue(), props.jwt().refreshTokenTtl())
                                .toString())
                .body(result.tokens());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                // JavaScript から読めなくする。XSS でトークンを盗まれないため
                .httpOnly(true)
                // 本番では必ず true。ブラウザは Secure 付き Cookie を HTTP の
                // オリジンでは保存しないため、HTTP のローカル開発でのみ false にする
                .secure(props.cookie().secure())
                // クロスサイトからの送信を禁止する。CSRF 対策
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
