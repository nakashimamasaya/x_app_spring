package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/** カーソルが復号できない、または limit が範囲外。 */
public class InvalidCursorException extends AppException {

    public InvalidCursorException() {
        super(HttpStatus.BAD_REQUEST, "urn:x-app-spring:problem:invalid-cursor", "Invalid cursor", "カーソルの形式が正しくありません。");
    }
}
