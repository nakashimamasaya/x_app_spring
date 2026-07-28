package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 更新リクエストで変更対象が 1 つも指定されていない。
 *
 * <p>api/openapi.yaml の UpdateProfileRequest は minProperties: 1。
 * Bean Validation では「最低 1 つ非 null」を表現できないためサービス層で弾く。
 */
public class EmptyUpdateException extends AppException {

    public EmptyUpdateException() {
        super(
                HttpStatus.BAD_REQUEST,
                "urn:x-app-spring:problem:validation-failed",
                "Validation failed",
                "変更する項目を 1 つ以上指定してください。");
    }
}
