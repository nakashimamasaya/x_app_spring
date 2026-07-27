package com.example.xapp.timeline;

import com.example.xapp.common.AuthenticatedUser;
import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.post.dto.PostResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** api/openapi.yaml の timeline タグに対応する。 */
@RestController
@RequestMapping("/timeline")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    /** 認証必須。 */
    @GetMapping("/home")
    public CursorPage<PostResponse> home(
            @AuthenticatedUser CurrentUser viewer,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return timelineService.home(viewer, cursor, limit);
    }

    /** 認証不要。認証して呼んだ場合のみ likedByMe が入る。 */
    @GetMapping("/public")
    public CursorPage<PostResponse> publicTimeline(
            @AuthenticatedUser CurrentUser viewer,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return timelineService.publicTimeline(viewer, cursor, limit);
    }
}
