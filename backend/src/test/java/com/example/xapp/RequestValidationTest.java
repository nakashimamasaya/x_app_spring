package com.example.xapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.xapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
// Boot 4 でパッケージが再編された。3.x の
// org.springframework.boot.test.autoconfigure.web.servlet は存在しない。
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * リクエストのバリデーションとパス変数の型変換が、api/openapi.yaml のとおり
 * 400 を返すことを確認する。
 *
 * <p><strong>セキュリティフィルタを外している。</strong> SecurityFilterChain の実装は
 * フェーズ3 のため、現状は既定設定が全リクエストを 401 で拒否してしまい、
 * バリデーションまで到達しない。認証・認可そのものの検証はフェーズ3 で行う。
 *
 * <p>ここで検証するのはサービス層より手前で完結する振る舞いだけなので、
 * サービスが未実装でも green になる。
 */
@AutoConfigureMockMvc(addFilters = false)
class RequestValidationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(
            strings = {
                // username が短すぎる / 長すぎる / 使用できない文字
                """
                {"username":"ab","email":"a@example.com","password":"password123","displayName":"A"}""",
                """
                {"username":"abcdefghijklmnopqrstu","email":"a@example.com","password":"password123","displayName":"A"}""",
                """
                {"username":"has-hyphen","email":"a@example.com","password":"password123","displayName":"A"}""",
                // email の形式が不正
                """
                {"username":"alice","email":"not-an-email","password":"password123","displayName":"A"}""",
                // password が 8 文字未満
                """
                {"username":"alice","email":"a@example.com","password":"short","displayName":"A"}""",
                // displayName が空
                """
                {"username":"alice","email":"a@example.com","password":"password123","displayName":""}""",
                // 必須フィールドの欠落
                """
                {"username":"alice"}"""
            })
    void 登録リクエストのバリデーション違反は400になる(String body) throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                """
                {"username":"alice","password":"short"}""",
                """
                {"username":"","password":"password123"}""",
                """
                {"username":"alice"}"""
            })
    void ログインリクエストのバリデーション違反は400になる(String body) throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // 投稿・プロフィール更新のバリデーションはここでテストできない。
    //
    // これらは @AuthenticatedUser CurrentUser を引数に取り、その引数リゾルバが
    // @Valid の評価より先に UnsupportedOperationException を投げるため、
    // バリデーションまで到達しない（引数解決はハンドラ実行前に全パラメータ分行われる）。
    //
    // フェーズ3 でリゾルバを実装した後に、認証まわりのテストと合わせて追加する。

    @Test
    void 壊れたJSONは400になる() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 存在しない UUID は 404 だが、UUID の形式ですらない値は型変換が失敗して 400 になる。
     * api/openapi.yaml でこの 2 つを別々に定義している根拠。
     */
    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "01912d4e-1a2b-7c3d-8e4f"})
    void postIdがUUID形式でなければ400になる(String malformed) throws Exception {
        mockMvc.perform(get("/posts/{postId}", malformed)).andExpect(status().isBadRequest());
    }
}
