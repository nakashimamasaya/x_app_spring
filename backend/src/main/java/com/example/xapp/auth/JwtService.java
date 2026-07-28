package com.example.xapp.auth;

import com.example.xapp.common.AppProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Access Token（JWT / HS256）の発行と検証。
 *
 * <p>対称鍵を使うのは、認可サーバーを別に立てず自分で発行・検証するため。
 * 外部に検証させる必要が出たら非対称鍵（RS256）に切り替える。
 *
 * <p>Refresh Token は JWT ではない。ランダム値を発行し、ハッシュを DB に持つ
 * （{@link AuthServiceImpl} 参照）。失効させられる必要があるため。
 */
@Service
public class JwtService {

    /** username を入れる独自クレーム。ユーザー識別は sub（UUID）で行う。 */
    static final String CLAIM_USERNAME = "username";

    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final AppProperties props;

    public JwtService(AppProperties props) {
        this.props = props;
        byte[] key = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec spec = new SecretKeySpec(key, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(spec));
        this.decoder = NimbusJwtDecoder.withSecretKey(spec).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public String issueAccessToken(UUID userId, String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .subject(userId.toString())
                        .claim(CLAIM_USERNAME, username)
                        .issuedAt(now)
                        .expiresAt(now.plus(props.jwt().accessTokenTtl()))
                        .build();
        JwsHeader header = JwsHeader.with(() -> JWSAlgorithm.HS256.getName()).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return props.jwt().accessTokenTtl().toSeconds();
    }

    /** SecurityFilterChain の JwtDecoder として使う。 */
    NimbusJwtDecoder decoder() {
        return decoder;
    }

    static UUID userIdOf(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    static String usernameOf(Jwt jwt) {
        return jwt.getClaimAsString(CLAIM_USERNAME);
    }
}
