package com.example.xapp.auth;

import com.example.xapp.common.AppProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 認可ルールは api/openapi.yaml の security 定義と 1 対 1 で対応させる。
 * 契約側を変えたらここも必ず直すこと。
 *
 * <p>BFF を挟まずブラウザから直接叩かれるため、CORS をここで明示設定する。
 */
@Configuration
public class SecurityConfig {

    /**
     * 認証不要のエンドポイント。
     *
     * <p>これ以外は全て認証必須にする（ホワイトリスト方式）。新しいエンドポイントを
     * 足したときに、うっかり公開してしまう事故を防ぐため。
     */
    private static final String[] PUBLIC_GET = {
        "/users/*", "/users/*/posts", "/users/*/followers", "/users/*/following",
        "/posts/*", "/timeline/public", "/actuator/health"
    };

    /**
     * {@code /users/*} は {@code /users/me} にもマッチしてしまうため、先に認証必須として
     * 除外する。Spring Security は宣言順に評価するので、この順序が意味を持つ。
     *
     * <p>これを忘れると自分のプロフィールが未認証で読めてしまう。実際にテストで検出した。
     */
    private static final String ME = "/users/me";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        return http
                // トークンをヘッダで送る API なので CSRF トークンは不要。
                // Refresh Cookie は SameSite=Strict かつ Path を /auth に限定して防ぐ。
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/auth/**")
                                        .permitAll()
                                        // PUBLIC_GET の /users/* より先に評価させる
                                        .requestMatchers(ME)
                                        .authenticated()
                                        .requestMatchers(HttpMethod.GET, PUBLIC_GET)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtService.decoder())))
                .build();
    }

    /**
     * 認証不要のエンドポイントでも、トークンがあれば {@code likedByMe} などを埋めたい。
     * Resource Server は Authorization ヘッダがあれば検証してコンテキストに載せるため、
     * permitAll でも認証情報は利用できる。
     */
    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService.decoder();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        // ワイルドカードは使わない。allowCredentials と併用できないうえ、
        // public な API を無条件に開くことになるため。
        config.setAllowedOrigins(props.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Refresh Cookie を送受信するために必要
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
