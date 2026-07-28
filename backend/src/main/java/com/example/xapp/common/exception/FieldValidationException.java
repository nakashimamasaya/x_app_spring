package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Bean Validation では表現できない検証をサービス層で行った結果のエラー。
 *
 * <p>レスポンスの形を {@code @Valid} 由来の 400 と揃えるため、違反フィールド名を持つ
 * （api/openapi.yaml の ValidationProblem に {@code errors} 配列がある）。
 */
public class FieldValidationException extends AppException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(
                HttpStatus.BAD_REQUEST,
                "urn:x-app-spring:problem:validation-failed",
                "Validation failed",
                message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
