package com.example.xapp.common.dto;

import java.util.List;

/**
 * カーソルページングのレスポンス。api/openapi.yaml の PostPage / UserPage に対応する。
 *
 * <p>{@code nextCursor} は次ページが無い場合 {@code null}。空文字にはしない
 * （JSON 上で「無い」ことを型で表現するため）。
 */
public record CursorPage<T>(List<T> items, String nextCursor) {

    public CursorPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
