package com.example.xapp.common.exception;

import org.springframework.http.HttpStatus;

/** username の重複（INV-7）。 */
public class UsernameTakenException extends AppException {

    public UsernameTakenException() {
        super(HttpStatus.CONFLICT, "urn:x-app-spring:problem:username-taken", "Username already taken", "この username は既に使われています。");
    }
}
