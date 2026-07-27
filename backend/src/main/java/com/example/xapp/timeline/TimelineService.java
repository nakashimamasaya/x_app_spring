package com.example.xapp.timeline;

import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.post.dto.PostResponse;

/**
 * タイムライン取得。実装はフェーズ3。
 *
 * <p>MVP は fan-out on read（購読側で都度集計）。fan-out on write はスコープ外。
 */
public interface TimelineService {

    /** 自分自身とフォロー中ユーザーの投稿。認証必須。 */
    CursorPage<PostResponse> home(CurrentUser viewer, String cursor, int limit);

    /** 全ユーザーの投稿。{@code viewer} は未認証なら {@code null}。 */
    CursorPage<PostResponse> publicTimeline(CurrentUser viewer, String cursor, int limit);
}
