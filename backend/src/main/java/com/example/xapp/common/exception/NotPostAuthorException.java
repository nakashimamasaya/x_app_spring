package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/** 他人の投稿を削除しようとした（INV-4）。 */
public class NotPostAuthorException extends AppException {

    public NotPostAuthorException() {
        super(HttpStatus.FORBIDDEN, "urn:x-app-spring:problem:not-post-author", "Not the author", "自分の投稿のみ削除できます。");
    }
}
