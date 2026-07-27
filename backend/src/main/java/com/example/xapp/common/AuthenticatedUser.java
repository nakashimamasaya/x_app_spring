package com.example.xapp.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * コントローラの引数に付けて、認証済みユーザーを {@link CurrentUser} として受け取る。
 *
 * <p>認証不要のエンドポイントでは未認証時に {@code null} が入る。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedUser {}
