package com.example.xapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ログインは username のみで行う。email ではログインできない（docs/adr/0007）。
 *
 * <p>username の形式違反はここで弾かず、認証失敗として扱う。
 * 「存在しない username」と「パスワード誤り」を区別しないため（ユーザー列挙攻撃対策）。
 */
public record LoginRequest(
        @NotBlank String username, @NotBlank @Size(min = 8, max = 72) String password) {}
