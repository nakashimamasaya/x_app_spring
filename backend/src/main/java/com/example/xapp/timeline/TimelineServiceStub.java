package com.example.xapp.timeline;

import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.post.dto.PostResponse;
import org.springframework.stereotype.Service;

/**
 * TimelineService の未実装スタブ。フェーズ3 で本実装に置き換える。
 *
 * <p>テストが「仕様どおりに落ちる」ことを確認できるよう、コンテキストは起動させたうえで
 * 呼び出し時に UnsupportedOperationException を投げる。
 */
@Service
public class TimelineServiceStub implements TimelineService {

    @Override
    public CursorPage<PostResponse> home(CurrentUser viewer, String cursor, int limit) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public CursorPage<PostResponse> publicTimeline(CurrentUser viewer, String cursor, int limit) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

}
