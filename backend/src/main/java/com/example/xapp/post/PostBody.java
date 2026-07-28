package com.example.xapp.post;

import com.example.xapp.common.exception.FieldValidationException;

/**
 * 投稿本文の正規化と検証（INV-6）。
 *
 * <p><strong>文字数は Unicode コードポイント数で数える。</strong>
 * {@code String.length()} は UTF-16 単位で数えるため絵文字が 2 文字になり、
 * PostgreSQL の {@code length()}（コードポイント数）と判定がズレる。
 * ズレると、アプリを通ったのに DB の CHECK 制約で落ちるという事故になる。
 *
 * <p>この理由で {@code @Size} を使わず、ここで検証している。
 */
public final class PostBody {

    public static final int MAX_CODE_POINTS = 280;

    private PostBody() {}

    /**
     * 前後の空白を除去したうえで 1〜280 コードポイントであることを検証する。
     *
     * @return 正規化済みの本文
     */
    public static String normalize(String raw) {
        String trimmed = raw == null ? "" : raw.strip();
        int length = trimmed.codePointCount(0, trimmed.length());

        if (length == 0) {
            throw new FieldValidationException("body", "本文を入力してください。");
        }
        if (length > MAX_CODE_POINTS) {
            throw new FieldValidationException(
                    "body", "本文は 1〜%d 文字で入力してください。".formatted(MAX_CODE_POINTS));
        }
        return trimmed;
    }
}
