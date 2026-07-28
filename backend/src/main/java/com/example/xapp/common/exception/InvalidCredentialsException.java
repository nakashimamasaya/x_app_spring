package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 認証失敗。
 *
 * <p><strong>username が存在しない場合とパスワード誤りで、この同一の例外を使う。</strong>
 * 区別するとユーザー列挙攻撃が成立するため。
 */
public class InvalidCredentialsException extends AppException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "urn:x-app-spring:problem:invalid-credentials", "Invalid credentials", "username またはパスワードが正しくありません。");
    }
}
