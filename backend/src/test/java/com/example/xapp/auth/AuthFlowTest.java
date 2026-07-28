package com.example.xapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.xapp.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;

/**
 * 認証フローの統合テスト。<strong>セキュリティフィルタを有効にしたまま</strong>実行する。
 *
 * <p>フェーズ2 では SecurityFilterChain が無く全て 401 になっていたため、
 * このレベルの検証はここが初めてになる。
 */
@AutoConfigureMockMvc
class AuthFlowTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    /**
     * 発行したトークンが認証を通ることを確認する。
     *
     * <p>{@code /users/me} のレスポンス本体は検証しない。UserService は次のスライスで
     * 実装するため、ここで到達すると UnsupportedOperationException になる。
     * 「401 で弾かれずハンドラまで到達した」ことが、ここで見たい振る舞い。
     */
    @Test
    void 発行したトークンで認証を通過できる() throws Exception {
        register("flowuser", "flow@example.com", "password123", "フロー");

        MvcResult login = login("flowuser", "password123").andExpect(status().isOk()).andReturn();
        String accessToken = accessTokenOf(login);

        assertThatThrownBy(
                        () ->
                                mockMvc.perform(
                                        get("/users/me")
                                                .header(
                                                        HttpHeaders.AUTHORIZATION,
                                                        "Bearer " + accessToken)))
                .hasRootCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 登録レスポンスに_email_が含まれない() throws Exception {
        register("noemail", "noemail@example.com", "password123", "ノーメール")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void username_が重複すると_409() throws Exception {
        register("dupuser", "dup1@example.com", "password123", "重複");

        register("dupuser", "dup2@example.com", "password123", "重複")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:x-app-spring:problem:username-taken"));
    }

    @Test
    void email_が重複すると_409() throws Exception {
        register("dupmail1", "same@example.com", "password123", "重複");

        register("dupmail2", "same@example.com", "password123", "重複")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:x-app-spring:problem:email-taken"));
    }

    /** ユーザー列挙攻撃対策。存在しない username とパスワード誤りを区別しない。 */
    @Test
    void 存在しないユーザーとパスワード誤りで同一のレスポンスを返す() throws Exception {
        register("enumtest", "enum@example.com", "password123", "列挙");

        String wrongPassword =
                login("enumtest", "wrong-password")
                        .andExpect(status().isUnauthorized())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String unknownUser =
                login("nosuchuser", "password123")
                        .andExpect(status().isUnauthorized())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    void ログインの_Refresh_Cookie_は_HttpOnly_かつパスが_auth_配下に限定される() throws Exception {
        register("cookieuser", "cookie@example.com", "password123", "クッキー");

        String setCookie =
                login("cookieuser", "password123")
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie)
                .contains("refresh_token=")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                // 通常の API リクエストに Cookie が載らないようパスを限定している
                .contains("Path=/api/v1/auth");
    }

    @Test
    void リフレッシュで新しいトークンが得られ古い_Refresh_は失効する() throws Exception {
        register("refresher", "refresher@example.com", "password123", "更新");
        Cookie refreshCookie = refreshCookieOf(login("refresher", "password123").andReturn());

        mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 使い終わった Refresh Token は失効している（INV-9 ローテーション）
        mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.type").value("urn:x-app-spring:problem:refresh-token-revoked"));
    }

    /** INV-10: 失効済みトークンの再利用を検知したら、そのユーザーの全セッションを切る。 */
    @Test
    void 失効済み_Refresh_の再利用で全セッションが失効する() throws Exception {
        register("stolen", "stolen@example.com", "password123", "盗難");
        Cookie first = refreshCookieOf(login("stolen", "password123").andReturn());

        MvcResult rotated =
                mockMvc.perform(post("/auth/refresh").cookie(first))
                        .andExpect(status().isOk())
                        .andReturn();
        Cookie second = refreshCookieOf(rotated);

        // 盗まれた古いトークンが再提示された
        mockMvc.perform(post("/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());

        // 正規のトークンも巻き添えで失効する（安全側に倒す）
        mockMvc.perform(post("/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
    }

    @Test
    void ログアウトすると_Refresh_が失効し_Cookie_が削除される() throws Exception {
        register("logouter", "logouter@example.com", "password123", "ログアウト");
        Cookie refreshCookie = refreshCookieOf(login("logouter", "password123").andReturn());

        mockMvc.perform(post("/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(
                        result ->
                                assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                                        .contains("Max-Age=0"));

        mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    /** 冪等。Cookie が無い状態で呼んでも 204。 */
    @Test
    void ログアウトは冪等である() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isNoContent());
    }

    @Test
    void 認証なしで_users_me_を叩くと_401() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void 壊れたトークンでは_401() throws Exception {
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 公開タイムラインは認証不要（api/openapi.yaml の security: [] に対応）。
     *
     * <p>TimelineService は次のスライスで実装するため、認証を通過すると
     * UnsupportedOperationException になる。401 で弾かれないことがここでの検証対象。
     */
    @Test
    void 公開タイムラインは認証なしで到達できる() {
        assertThatThrownBy(() -> mockMvc.perform(get("/timeline/public")))
                .hasRootCauseInstanceOf(UnsupportedOperationException.class);
    }

    // ---- ヘルパ ----------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions register(
            String username, String email, String password, String displayName) throws Exception {
        String body =
                """
                {"username":"%s","email":"%s","password":"%s","displayName":"%s"}"""
                        .formatted(username, email, password, displayName);
        return mockMvc.perform(
                post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password)
            throws Exception {
        String body = """
                {"username":"%s","password":"%s"}""".formatted(username, password);
        return mockMvc.perform(
                post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // ObjectMapper を注入しないのは、Spring Boot 4 が Jackson 3 を使い、
    // パッケージが com.fasterxml.jackson から tools.jackson に変わっているため。
    // JsonPath ならその差に依存しない。
    private String accessTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    /** MockMvc は Set-Cookie を自動で引き継がないので、手で取り出して次のリクエストに載せる。 */
    private Cookie refreshCookieOf(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(AuthController.REFRESH_COOKIE);
        assertThat(cookie).as("Refresh Cookie が発行されていること").isNotNull();
        return cookie;
    }
}
