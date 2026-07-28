package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/** 投稿が存在しないか削除済み（INV-5）。両者を区別せず 404 にする。 */
public class PostNotFoundException extends AppException {

    public PostNotFoundException() {
        super(HttpStatus.NOT_FOUND, "urn:x-app-spring:problem:post-not-found", "Post not found", "投稿が見つかりません。");
    }
}
