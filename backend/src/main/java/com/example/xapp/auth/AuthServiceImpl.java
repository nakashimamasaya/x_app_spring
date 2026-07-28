package com.example.xapp.auth;

import com.example.xapp.auth.dto.LoginRequest;
import com.example.xapp.auth.dto.RegisterRequest;
import com.example.xapp.auth.dto.TokenResponse;
import com.example.xapp.common.UuidV7;
import com.example.xapp.common.exception.EmailTakenException;
import com.example.xapp.common.exception.InvalidCredentialsException;
import com.example.xapp.common.exception.UsernameTakenException;
import com.example.xapp.user.UserRepository;
import com.example.xapp.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;

    public AuthServiceImpl(
            UserRepository users,
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokens) {
        this.users = users;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 事前チェックはユーザーに分かりやすいエラーを返すため。
        // 競合状態は下の DuplicateKeyException で拾う（DB の一意制約が最後の砦）
        if (users.existsByUsername(request.username())) {
            throw new UsernameTakenException();
        }
        if (users.existsByEmail(request.email())) {
            throw new EmailTakenException();
        }

        UUID id = UuidV7.generate();
        try {
            jdbc.update(
                    """
                    INSERT INTO users (id, username, email, password_hash, display_name, bio)
                    VALUES (?, ?, ?, ?, ?, '')
                    """,
                    id,
                    request.username(),
                    request.email(),
                    passwordEncoder.encode(request.password()),
                    request.displayName());
        } catch (DuplicateKeyException e) {
            // どちらが衝突したかは再問い合わせで判定する
            throw users.existsByUsername(request.username())
                    ? new UsernameTakenException()
                    : new EmailTakenException();
        }

        return users.findById(id)
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getBio(), u.getCreatedAt()))
                .orElseThrow();
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        var user = users.findByUsername(request.username()).orElse(null);

        // username が存在しない場合もパスワード誤りと同じ例外にする。
        // 区別するとユーザー列挙攻撃が成立するため。
        // また、存在しない場合もハッシュ照合を実行して応答時間の差を消す
        String storedHash = user == null ? DUMMY_HASH : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), storedHash);
        if (user == null || !matches) {
            throw new InvalidCredentialsException();
        }

        return issueFor(user.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        var rotated = refreshTokens.rotate(refreshTokenValue);
        var user = users.findById(rotated.userId()).orElseThrow(InvalidCredentialsException::new);
        return new AuthResult(
                TokenResponse.bearer(
                        jwtService.issueAccessToken(user.getId(), user.getUsername()),
                        jwtService.accessTokenTtlSeconds()),
                rotated.newRawToken());
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokens.revoke(refreshTokenValue);
    }

    private AuthResult issueFor(UUID userId, String username) {
        return new AuthResult(
                TokenResponse.bearer(
                        jwtService.issueAccessToken(userId, username),
                        jwtService.accessTokenTtlSeconds()),
                refreshTokens.issue(userId));
    }

    /**
     * 存在しないユーザーでもハッシュ照合を走らせるためのダミー。
     * これが無いと「存在しない username は即座に返る」というタイミング差から
     * ユーザーの有無が推測できてしまう。
     */
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
}
