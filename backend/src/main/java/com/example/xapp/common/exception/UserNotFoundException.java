package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/** 存在しない username を指定された。 */
public class UserNotFoundException extends AppException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "urn:x-app-spring:problem:user-not-found", "User not found", "ユーザーが見つかりません。");
    }
}
