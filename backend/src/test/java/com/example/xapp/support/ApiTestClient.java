package com.example.xapp.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * テストからユーザーを作ってログインするための補助。
 *
 * <p>ObjectMapper を使わないのは、Spring Boot 4 が Jackson 3 を使い
 * パッケージが {@code tools.jackson} に変わっているため。JsonPath なら差を吸収できる。
 */
public final class ApiTestClient {

    private final MockMvc mockMvc;

    public ApiTestClient(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** 登録してログインし、Authorization ヘッダに使う値を返す。 */
    public String registerAndLogin(String username) throws Exception {
        register(username);
        return login(username);
    }

    public void register(String username) throws Exception {
        String body =
                """
                {"username":"%s","email":"%s@example.com","password":"password123","displayName":"%s"}"""
                        .formatted(username, username, username);
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(r -> {
                    if (r.getResponse().getStatus() != 201) {
                        throw new IllegalStateException(
                                "登録に失敗: " + r.getResponse().getContentAsString());
                    }
                });
    }

    public String login(String username) throws Exception {
        String body = """
                {"username":"%s","password":"password123"}""".formatted(username);
        String json =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return "Bearer " + JsonPath.<String>read(json, "$.accessToken");
    }

    /** リクエストに Authorization ヘッダを付ける。 */
    public static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, token);
    }
}
