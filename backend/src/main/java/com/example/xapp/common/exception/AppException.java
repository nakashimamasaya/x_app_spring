package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * このアプリが意図的に投げる例外の基底。
 *
 * <p>{@code type} は api/openapi.yaml の Problem.type と一致させること。
 * クライアントはこの値で分岐するので、変えると契約違反になる。
 */
public abstract class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String title;

    protected AppException(HttpStatus status, String type, String title, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }
}
