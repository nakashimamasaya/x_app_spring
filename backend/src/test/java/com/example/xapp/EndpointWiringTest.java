package com.example.xapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.xapp.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * api/openapi.yaml で定義した 19 オペレーションが、すべて実際にハンドラとして
 * 登録されていることを確認する。
 *
 * <p>実装の中身ではなく「契約とルーティングの対応」を検証するテストなので、
 * サービスが未実装のフェーズ2 時点でも green になる。
 *
 * <p>これが落ちるときは、パスの綴り間違いか HTTP メソッドの取り違えを意味する。
 */
class EndpointWiringTest extends AbstractIntegrationTest {

    /** api/openapi.yaml の paths と 1:1 で対応する。増減したらこの表も直すこと。 */
    private static final List<String> EXPECTED =
            List.of(
                    "POST /auth/register",
                    "POST /auth/login",
                    "POST /auth/refresh",
                    "POST /auth/logout",
                    "GET /users/me",
                    "PATCH /users/me",
                    "GET /users/{username}",
                    "GET /users/{username}/posts",
                    "POST /users/{username}/follow",
                    "DELETE /users/{username}/follow",
                    "GET /users/{username}/followers",
                    "GET /users/{username}/following",
                    "POST /posts",
                    "GET /posts/{postId}",
                    "DELETE /posts/{postId}",
                    "POST /posts/{postId}/like",
                    "DELETE /posts/{postId}/like",
                    "GET /timeline/home",
                    "GET /timeline/public");

    // Actuator が同じ型の Bean を 3 つ追加するため、名前で明示的に指定する
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void 契約の全オペレーションがハンドラとして登録されている() {
        Set<String> registered =
                handlerMapping.getHandlerMethods().keySet().stream()
                        .flatMap(
                                info ->
                                        info.getPatternValues().stream()
                                                .flatMap(
                                                        path ->
                                                                info.getMethodsCondition()
                                                                        .getMethods()
                                                                        .stream()
                                                                        .map(m -> m + " " + path)))
                        .collect(Collectors.toSet());

        assertThat(registered).containsAll(EXPECTED);
    }

    @Test
    void 契約に無いエンドポイントが生えていない() {
        Set<String> appEndpoints =
                handlerMapping.getHandlerMethods().entrySet().stream()
                        .filter(e -> e.getValue().getBeanType().getName().startsWith("com.example.xapp"))
                        .flatMap(
                                e ->
                                        e.getKey().getPatternValues().stream()
                                                .flatMap(
                                                        path ->
                                                                e.getKey()
                                                                        .getMethodsCondition()
                                                                        .getMethods()
                                                                        .stream()
                                                                        .map(m -> m + " " + path)))
                        .collect(Collectors.toSet());

        assertThat(appEndpoints).containsExactlyInAnyOrderElementsOf(EXPECTED);
    }
}
