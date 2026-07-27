package com.example.xapp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 制約は api/openapi.yaml の RegisterRequest スキーマと一致させること。 */
public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$") String username,
        @NotBlank @Email @Size(max = 254) String email,
        // 上限 72 は BCrypt の仕様上の制限
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 1, max = 50) String displayName) {}
