package com.example.xapp.user;

import static com.example.xapp.support.ApiTestClient.as;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.xapp.support.AbstractIntegrationTest;
import com.example.xapp.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** プロフィールとフォロー関係の統合テスト。api/openapi.yaml の users タグに対応する。 */
@AutoConfigureMockMvc
class UserApiTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private ApiTestClient client;

    @BeforeEach
    void setUp() {
        client = new ApiTestClient(mockMvc);
    }

    @Test
    void 自分のプロフィールを取得できる() throws Exception {
        String token = client.registerAndLogin("meuser");

        mockMvc.perform(as(get("/users/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("meuser"))
                .andExpect(jsonPath("$.bio").value(""))
                // email は自分自身の情報でも返さない（docs/adr/0004）
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void プロフィールを更新できる() throws Exception {
        String token = client.registerAndLogin("edituser");

        mockMvc.perform(
                        as(patch("/users/me"), token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"displayName":"新しい名前","bio":"自己紹介"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("新しい名前"))
                .andExpect(jsonPath("$.bio").value("自己紹介"));
    }

    /** 省略したフィールドは変更されない。 */
    @Test
    void 指定しなかった項目は変更されない() throws Exception {
        String token = client.registerAndLogin("partialuser");
        mockMvc.perform(
                as(patch("/users/me"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"名前A","bio":"紹介A"}"""));

        mockMvc.perform(
                        as(patch("/users/me"), token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"bio":"紹介B"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("名前A"))
                .andExpect(jsonPath("$.bio").value("紹介B"));
    }

    /** api/openapi.yaml の minProperties: 1 に対応。 */
    @Test
    void 空の更新リクエストは400() throws Exception {
        String token = client.registerAndLogin("emptyupdate");

        mockMvc.perform(
                        as(patch("/users/me"), token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 他人のプロフィールを認証なしで取得でき_isFollowing_は_null_になる() throws Exception {
        client.register("publicprofile");

        mockMvc.perform(get("/users/{username}", "publicprofile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("publicprofile"))
                .andExpect(jsonPath("$.postCount").value(0))
                .andExpect(jsonPath("$.followerCount").value(0))
                // 未認証は false ではなく null（不明）
                .andExpect(jsonPath("$.isFollowing").doesNotExist());
    }

    @Test
    void 存在しないユーザーは404() throws Exception {
        mockMvc.perform(get("/users/{username}", "nosuchuser"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:x-app-spring:problem:user-not-found"));
    }

    @Test
    void フォローするとカウントと_isFollowing_に反映される() throws Exception {
        String aliceToken = client.registerAndLogin("followalice");
        client.register("followbob");

        mockMvc.perform(as(post("/users/{username}/follow", "followbob"), aliceToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(as(get("/users/{username}", "followbob"), aliceToken))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.isFollowing").value(true));

        mockMvc.perform(as(get("/users/{username}", "followalice"), aliceToken))
                .andExpect(jsonPath("$.followingCount").value(1));
    }

    /** 冪等（docs/adr/0005）。2 回目も 409 ではなく 204。 */
    @Test
    void 二重フォローは204で冪等() throws Exception {
        String token = client.registerAndLogin("dupfollower");
        client.register("dupfollowee");

        mockMvc.perform(as(post("/users/{username}/follow", "dupfollowee"), token))
                .andExpect(status().isNoContent());
        mockMvc.perform(as(post("/users/{username}/follow", "dupfollowee"), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(as(get("/users/{username}", "dupfollowee"), token))
                .andExpect(jsonPath("$.followerCount").value(1));
    }

    /** フォローしていない相手のアンフォローも 204。 */
    @Test
    void 未フォロー相手のアンフォローは204で冪等() throws Exception {
        String token = client.registerAndLogin("unfollower");
        client.register("neverfollowed");

        mockMvc.perform(as(delete("/users/{username}/follow", "neverfollowed"), token))
                .andExpect(status().isNoContent());
    }

    /** 「既にその状態」ではなく論理的にあり得ない要求なので冪等にしない（docs/adr/0005）。 */
    @Test
    void 自分自身のフォローは400() throws Exception {
        String token = client.registerAndLogin("selffollower");

        mockMvc.perform(as(post("/users/{username}/follow", "selffollower"), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:x-app-spring:problem:self-follow"));
    }

    @Test
    void 認証なしのフォローは401() throws Exception {
        client.register("targetuser");

        mockMvc.perform(post("/users/{username}/follow", "targetuser"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void フォロワー一覧とフォロー中一覧を取得できる() throws Exception {
        String aliceToken = client.registerAndLogin("listalice");
        client.register("listbob");
        mockMvc.perform(as(post("/users/{username}/follow", "listbob"), aliceToken));

        mockMvc.perform(get("/users/{username}/followers", "listbob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].username").value("listalice"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());

        mockMvc.perform(get("/users/{username}/following", "listalice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].username").value("listbob"));
    }

    @Test
    void 壊れたカーソルは400() throws Exception {
        client.register("cursoruser");

        mockMvc.perform(get("/users/{username}/followers", "cursoruser").param("cursor", "!!!broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:x-app-spring:problem:invalid-cursor"));
    }
}
