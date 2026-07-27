package com.example.xapp.post.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 本文の長さ検証は {@code @Size} では行わない。
 *
 * <p>{@code @Size} は {@code String.length()}（UTF-16 単位）で数えるため、絵文字が
 * 2 文字として扱われ、DB の {@code length()}（コードポイント数）と判定がズレる（INV-6）。
 * 前後の空白除去とコードポイント数の検証はサービス層で行う。
 */
public record CreatePostRequest(@NotBlank String body) {}
