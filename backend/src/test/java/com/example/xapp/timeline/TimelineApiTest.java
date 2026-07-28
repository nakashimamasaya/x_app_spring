package com.example.xapp.timeline;

import static com.example.xapp.support.ApiTestClient.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.xapp.support.AbstractIntegrationTest;
import com.example.xapp.support.ApiTestClient;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** タイムラインの統合テスト。api/openapi.yaml の timeline タグに対応する。 */
@AutoConfigureMockMvc
class TimelineApiTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private ApiTestClient client;

    @BeforeEach
    void setUp() {
        client = new ApiTestClient(mockMvc);
    }

    @Test
    void ホームには自分とフォロー中の投稿だけが並ぶ() throws Exception {
        String meToken = client.registerAndLogin("tl_me");
        String friendToken = client.registerAndLogin("tl_friend");
        String strangerToken = client.registerAndLogin("tl_stranger");

        createPost(meToken, "自分の投稿");
        createPost(friendToken, "友人の投稿");
        createPost(strangerToken, "他人の投稿");

        mockMvc.perform(as(post("/users/{u}/follow", "tl_friend"), meToken));

        String json =
                mockMvc.perform(as(get("/timeline/home"), meToken))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<String> bodies = JsonPath.read(json, "$.items[*].body");
        assertThat(bodies).containsExactlyInAnyOrder("自分の投稿", "友人の投稿");
    }

    @Test
    void ホームは新しい順に並ぶ() throws Exception {
        String token = client.registerAndLogin("tl_order");
        createPost(token, "1番目");
        createPost(token, "2番目");
        createPost(token, "3番目");

        String json =
                mockMvc.perform(as(get("/timeline/home"), token))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<String> bodies = JsonPath.read(json, "$.items[*].body");
        assertThat(bodies).containsExactly("3番目", "2番目", "1番目");
    }

    @Test
    void 削除済み投稿はタイムラインに出ない() throws Exception {
        String token = client.registerAndLogin("tl_deleted");
        String keptId = createPost(token, "残る投稿");
        String deletedId = createPost(token, "消える投稿");
        mockMvc.perform(as(delete("/posts/{id}", deletedId), token));

        String json =
                mockMvc.perform(as(get("/timeline/home"), token))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<String> ids = JsonPath.read(json, "$.items[*].id");
        assertThat(ids).contains(keptId).doesNotContain(deletedId);
    }

    @Test
    void ホームは認証必須() throws Exception {
        mockMvc.perform(get("/timeline/home")).andExpect(status().isUnauthorized());
    }

    @Test
    void 公開タイムラインは未認証で読めて_likedByMe_は_null() throws Exception {
        String token = client.registerAndLogin("tl_public");
        createPost(token, "公開投稿");

        mockMvc.perform(get("/timeline/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].likedByMe").doesNotExist());
    }

    /** limit + 1 件取得して次ページの有無を判定する方式の検証（docs/adr/0003）。 */
    @Test
    void カーソルページングで重複や取りこぼしなく全件たどれる() throws Exception {
        String token = client.registerAndLogin("tl_paging");
        for (int i = 1; i <= 5; i++) {
            createPost(token, "投稿" + i);
        }

        List<String> collected = new java.util.ArrayList<>();
        String cursor = null;
        for (int page = 0; page < 10; page++) {
            var request = as(get("/timeline/home"), token).param("limit", "2");
            if (cursor != null) {
                request = request.param("cursor", cursor);
            }
            String json = mockMvc.perform(request).andReturn().getResponse().getContentAsString();
            collected.addAll(JsonPath.read(json, "$.items[*].body"));
            cursor = JsonPath.read(json, "$.nextCursor");
            if (cursor == null) {
                break;
            }
        }

        assertThat(collected)
                .as("重複なく新しい順で 5 件全て取得できる")
                .containsExactly("投稿5", "投稿4", "投稿3", "投稿2", "投稿1");
    }

    @Test
    void 最終ページの_nextCursor_は_null() throws Exception {
        String token = client.registerAndLogin("tl_lastpage");
        createPost(token, "唯一の投稿");

        mockMvc.perform(as(get("/timeline/home"), token).param("limit", "20"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void limit_が範囲外なら400() throws Exception {
        String token = client.registerAndLogin("tl_limit");

        mockMvc.perform(as(get("/timeline/home"), token).param("limit", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(get("/timeline/home"), token).param("limit", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 壊れたカーソルは400() throws Exception {
        mockMvc.perform(get("/timeline/public").param("cursor", "!!!broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:x-app-spring:problem:invalid-cursor"));
    }

    private String createPost(String token, String body) throws Exception {
        String json =
                mockMvc.perform(
                                as(post("/posts"), token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"body\":\"%s\"}".formatted(body)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
