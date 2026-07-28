package com.example.xapp.auth;

import com.example.xapp.common.AppProperties;
import com.example.xapp.common.UuidV7;
import com.example.xapp.common.exception.RefreshTokenRevokedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token の発行・検証・失効。
 *
 * <p>Refresh Token は JWT ではなくランダム値。失効させられる必要があるため、
 * 状態を DB に持つ。<strong>生の値は保存せず SHA-256 ハッシュのみ</strong>を持ち、
 * DB が漏洩してもそのまま Refresh に使えないようにする。
 *
 * <p>使用のたびにローテーションし（INV-9）、失効済みトークンが再提示されたら
 * 盗難とみなしてそのユーザーの全トークンを失効させる（INV-10）。
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final JdbcTemplate jdbc;
    private final AppProperties props;
    private final PlatformTransactionManager transactionManager;

    public RefreshTokenService(
            JdbcTemplate jdbc, AppProperties props, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.props = props;
        this.transactionManager = transactionManager;
    }

    /** @return クライアントに返す生のトークン値。DB にはハッシュだけを保存する。 */
    @Transactional
    public String issue(UUID userId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        jdbc.update(
                """
                INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """,
                UuidV7.generate(),
                userId,
                hash(raw),
                java.sql.Timestamp.from(Instant.now().plus(props.jwt().refreshTokenTtl())));
        return raw;
    }

    /**
     * トークンを検証し、ローテーションして新しい生トークンを返す。
     *
     * @throws RefreshTokenRevokedException 不明・期限切れ・失効済みの場合。
     *     失効済みだった場合は盗難とみなし、そのユーザーの全トークンを失効させてから投げる
     */
    @Transactional
    public Rotated rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new RefreshTokenRevokedException();
        }
        var rows =
                jdbc.queryForList(
                        """
                        SELECT user_id, expires_at, revoked_at
                        FROM refresh_tokens WHERE token_hash = ?
                        """,
                        hash(rawToken));
        if (rows.isEmpty()) {
            throw new RefreshTokenRevokedException();
        }

        var row = rows.getFirst();
        UUID userId = (UUID) row.get("user_id");
        Instant expiresAt = ((java.sql.Timestamp) row.get("expires_at")).toInstant();

        if (row.get("revoked_at") != null) {
            // 既に失効したトークンが再提示された = 盗まれた可能性が高い。
            // そのユーザーのセッションを全て切る（INV-10）。
            //
            // この直後に例外を投げるため、同一トランザクションで失効させると
            // ロールバックで巻き戻ってしまう。別トランザクションで確定させる。
            revokeAllForInNewTransaction(userId);
            throw new RefreshTokenRevokedException();
        }
        if (expiresAt.isBefore(Instant.now())) {
            throw new RefreshTokenRevokedException();
        }

        revoke(rawToken);
        return new Rotated(userId, issue(userId));
    }

    /** 冪等。存在しないトークンを渡しても例外を投げない。 */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        jdbc.update(
                "UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = ? AND revoked_at IS NULL",
                hash(rawToken));
    }

    @Transactional
    public void revokeAllFor(UUID userId) {
        jdbc.update(
                "UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = ? AND revoked_at IS NULL",
                userId);
    }

    /**
     * 盗難検知時の全失効を、呼び出し側のロールバックに巻き込まれずに確定させる。
     *
     * <p>自己呼び出しでは Spring の AOP プロキシを通らず伝播設定が効かないため、
     * {@link TransactionTemplate} で明示的に新しいトランザクションを開始する。
     */
    private void revokeAllForInNewTransaction(UUID userId) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> revokeAllFor(userId));
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder()
                    .encodeToString(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 が利用できません", e);
        }
    }

    public record Rotated(UUID userId, String newRawToken) {}
}
