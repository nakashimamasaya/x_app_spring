package com.example.xapp.timeline;

import com.example.xapp.common.AppProperties;
import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.PageLimit;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.post.PostQueryRepository;
import com.example.xapp.post.dto.PostResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MVP は fan-out on read。購読側で都度集計するので、投稿時の書き込み増幅が無い。
 * fan-out on write はスコープ外（docs/requirements.md）。
 */
@Service
public class TimelineServiceImpl implements TimelineService {

    private final PostQueryRepository postQuery;
    private final AppProperties props;

    public TimelineServiceImpl(PostQueryRepository postQuery, AppProperties props) {
        this.postQuery = postQuery;
        this.props = props;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> home(CurrentUser viewer, String cursor, int limit) {
        return postQuery.homeOf(viewer.id(), viewer, cursor, PageLimit.validate(limit, props));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> publicTimeline(CurrentUser viewer, String cursor, int limit) {
        return postQuery.publicTimeline(viewer, cursor, PageLimit.validate(limit, props));
    }
}
