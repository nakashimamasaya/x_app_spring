package com.example.xapp.common;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml の {@code app.*} を型付きで受け取る。
 *
 * <p>秘密の値は yaml に直書きせず環境変数から入る（{@code ${JWT_SECRET}}）。
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Pagination pagination, Cookie cookie) {

    /**
     * Refresh Cookie の属性。
     *
     * @param secure HTTPS でのみ送信させるか。<strong>本番では必ず true。</strong>
     *     ブラウザは Secure 付き Cookie を HTTP のオリジンでは保存しないため、
     *     HTTP で動かすローカル開発と E2E でのみ false にする
     */
    public record Cookie(boolean secure) {}

    /**
     * @param secret HS256 の署名鍵。32 バイト以上必須
     */
    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {}

    /** BFF を挟まない構成では必須。ワイルドカードは使わない。 */
    public record Cors(List<String> allowedOrigins) {}

    public record Pagination(int defaultLimit, int maxLimit) {}
}
