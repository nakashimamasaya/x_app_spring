package com.example.xapp.common;

import com.example.xapp.common.exception.InvalidCursorException;

/**
 * 一覧の {@code limit} を検証する。
 *
 * <p>契約（api/openapi.yaml）は 1〜100。範囲外は 400 で、カーソル不正と同じ扱いにする
 * （どちらも「ページング指定が不正」なので、クライアントの分岐を増やさない）。
 *
 * <p>上限を設けるのは、1 リクエストで全件取得されて DB とメモリを圧迫するのを防ぐため。
 */
public final class PageLimit {

    private PageLimit() {}

    public static int validate(int limit, AppProperties props) {
        if (limit < 1 || limit > props.pagination().maxLimit()) {
            throw new InvalidCursorException();
        }
        return limit;
    }
}
