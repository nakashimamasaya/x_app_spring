package com.example.xapp.auth;

import com.example.xapp.auth.dto.LoginRequest;
import com.example.xapp.auth.dto.RegisterRequest;
import com.example.xapp.auth.dto.TokenResponse;
import com.example.xapp.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
 * <p>Refresh Token は Cookie で扱う。名前とパス（{@code /api/v1/auth} 配下に限定）は
 * 契約で決まっているので勝手に変えない。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Refresh Token の Cookie 名。契約（openapi.yaml の Set-Cookie 例）と一致させること。 */
    public static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }
}
