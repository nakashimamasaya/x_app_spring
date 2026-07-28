package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/** 自己フォロー（INV-1）。冪等にせず 400 にするのは、論理的にあり得ない要求だから。 */
public class SelfFollowException extends AppException {

    public SelfFollowException() {
        super(HttpStatus.BAD_REQUEST, "urn:x-app-spring:problem:self-follow", "Cannot follow yourself", "自分自身をフォローすることはできません。");
    }
}
