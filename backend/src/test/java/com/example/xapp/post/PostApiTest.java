package com.example.xapp.post;

import static com.example.xapp.support.ApiTestClient.as;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.xapp.support.AbstractIntegrationTest;
import com.example.xapp.support.ApiTestClient;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 投稿といいねの統合テスト。api/openapi.yaml の posts タグに対応する。 */
@AutoConfigureMockMvc
class PostApiTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private ApiTestClient client;

    @BeforeEach
    void setUp() {
        client = new ApiTestClient(mockMvc);
    }

    @Test
    void 投稿を作成して取得できる() throws Exception {
        String token = client.registerAndLogin("poster");

        String id = createPost(token, "はじめての投稿です。");

        mockMvc.perform(as(get("/posts/{id}", id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("はじめての投稿です。"))
                .andExpect(jsonPath("$.author.username").value("poster"))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false));
    }

    /** 未認証で見た場合、likedByMe は false ではなく null（不明）。 */
    @Test
    void 未認証で投稿を見ると_likedByMe_は_null() throws Exception {
        String token = client.registerAndLogin("anonviewed");
        String id = createPost(token, "誰でも読める投稿");

        mockMvc.perform(get("/posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").doesNotExist());
    }

    @Test
    void 本文の前後の空白は除去される() throws Exception {
        String token = client.registerAndLogin("trimmer");

        String id = createPost(token, "   前後に空白   ");

        mockMvc.perform(as(get("/posts/{id}", id), token))
                .andExpect(jsonPath("$.body").value("前後に空白"));
    }

    @Test
    void 空白のみの本文は400() throws Exception {
        String token = client.registerAndLogin("blankbody");

        mockMvc.perform(
                        as(post("/posts"), token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"body":"    "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("body"));
    }

    @Test
    void 本文がちょうど280文字なら投稿できる() throws Exception {
        String token = client.registerAndLogin("exact280");

        String id = createPost(token, "あ".repeat(280));

        mockMvc.perform(as(get("/posts/{id}", id), token)).andExpect(status().isOk());
    }

    @Test
    void 本文が281文字なら400() throws Exception {
        String token = client.registerAndLogin("over280");

        mockMvc.perform(
                        as(post("/posts"), token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"%s\"}".formatted("あ".repeat(281))))
                .andExpect(status().isBadRequest());
    }

    /**
     * 絵文字はサロゲートペアなので String.length() では 2 になるが、
     * コードポイント数では 1（INV-6）。280 個は投稿できなければならない。
     */
    @Test
    void 絵文字を280個投稿できる() throws Exception {
        String token = client.registerAndLogin("emojipost");

        String id = createPost(token, "😀".repeat(280));

        mockMvc.perform(as(get("/posts/{id}", id), token)).andExpect(status().isOk());
    }

    @Test
    void 認証なしの投稿は401() throws Exception {
        mockMvc.perform(
                        post("/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"body":"匿名投稿"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 自分の投稿を削除できて削除後は404() throws Exception {
        String token = client.registerAndLogin("deleter");
        String id = createPost(token, "消される投稿");

        mockMvc.perform(as(delete("/posts/{id}", id), token)).andExpect(status().isNoContent());

        mockMvc.perform(as(get("/posts/{id}", id), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:x-app-spring:problem:post-not-found"));
    }

    @Test
    void 他人の投稿は削除できず403() throws Exception {
        String authorToken = client.registerAndLogin("realauthor");
        String otherToken = client.registerAndLogin("intruder");
        String id = createPost(authorToken, "他人の投稿");

        mockMvc.perform(as(delete("/posts/{id}", id), otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:x-app-spring:problem:not-post-author"));
    }

    /** 削除は冪等にしない（docs/adr/0005）。2 回目は 404。 */
    @Test
    void 削除済み投稿の再削除は404() throws Exception {
        String token = client.registerAndLogin("doubledelete");
        String id = createPost(token, "二度消し");
        mockMvc.perform(as(delete("/posts/{id}", id), token));

        mockMvc.perform(as(delete("/posts/{id}", id), token)).andExpect(status().isNotFound());
    }

    @Test
    void いいねするとカウントに反映される() throws Exception {
        String authorToken = client.registerAndLogin("likedauthor");
        String likerToken = client.registerAndLogin("liker");
        String id = createPost(authorToken, "いいねされる投稿");

        mockMvc.perform(as(post("/posts/{id}/like", id), likerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(as(get("/posts/{id}", id), likerToken))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));

        // 著者から見ると自分ではいいねしていない
        mockMvc.perform(as(get("/posts/{id}", id), authorToken))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));
    }

    /** 冪等（docs/adr/0005）。 */
    @Test
    void 二重いいねは204で件数も増えない() throws Exception {
        String token = client.registerAndLogin("doubleliker");
        String id = createPost(token, "二重いいね");

        mockMvc.perform(as(post("/posts/{id}/like", id), token)).andExpect(status().isNoContent());
        mockMvc.perform(as(post("/posts/{id}/like", id), token)).andExpect(status().isNoContent());

        mockMvc.perform(as(get("/posts/{id}", id), token)).andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    void いいねしていない投稿の解除も204で冪等() throws Exception {
        String token = client.registerAndLogin("unliker");
        String id = createPost(token, "未いいね");

        mockMvc.perform(as(delete("/posts/{id}/like", id), token))
                .andExpect(status().isNoContent());
    }

    /** 自分の投稿にもいいねできる（制約を置いていない）。 */
    @Test
    void 自分の投稿にもいいねできる() throws Exception {
        String token = client.registerAndLogin("selfliker");
        String id = createPost(token, "自画自賛");

        mockMvc.perform(as(post("/posts/{id}/like", id), token)).andExpect(status().isNoContent());

        mockMvc.perform(as(get("/posts/{id}", id), token))
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    void 存在しない投稿へのいいねは404() throws Exception {
        String token = client.registerAndLogin("ghostliker");

        mockMvc.perform(
                        as(post("/posts/{id}/like", "01912d4e-1a2b-7c3d-8e4f-5a6b7c8d9e0f"), token))
                .andExpect(status().isNotFound());
    }

    private String createPost(String token, String body) throws Exception {
        String json =
                mockMvc.perform(
                                as(post("/posts"), token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"body\":\"%s\"}"
                                                        .formatted(body.replace("\"", "\\\""))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
