package com.example.xapp.common;

import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link AuthenticatedUser} 付き引数に {@link CurrentUser} を注入する。
 *
 * <p>認証不要のエンドポイントでは未認証時に {@code null} が入る。呼び出し側は
 * その場合 {@code isFollowing} / {@code likedByMe} を {@code null}（不明）にする。
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    /** JWT の username クレーム名。JwtService と一致させること。 */
    private static final String CLAIM_USERNAME = "username";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedUser.class)
                && CurrentUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return new CurrentUser(
                UUID.fromString(jwt.getSubject()), jwt.getClaimAsString(CLAIM_USERNAME));
    }
}
