package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/** email の重複（INV-7）。 */
public class EmailTakenException extends AppException {

    public EmailTakenException() {
        super(HttpStatus.CONFLICT, "urn:x-app-spring:problem:email-taken", "Email already taken", "この email は既に使われています。");
    }
}
