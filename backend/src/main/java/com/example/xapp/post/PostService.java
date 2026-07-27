package com.example.xapp.post;

import com.example.xapp.common.CurrentUser;
import com.example.xapp.post.dto.CreatePostRequest;
import com.example.xapp.post.dto.PostResponse;
import java.util.UUID;

/** 投稿といいねのユースケース。実装はフェーズ3。 */
public interface PostService {

    PostResponse create(CurrentUser author, CreatePostRequest request);

    /** 削除済みは存在しないものとして扱う（INV-5）。 */
    PostResponse get(UUID postId, CurrentUser viewer);

    /** 著者本人のみ。他人の投稿なら例外（INV-4）。削除済み・不在は例外で、冪等ではない。 */
    void delete(UUID postId, CurrentUser viewer);

    /** 冪等。既にいいね済みでも例外を投げない（docs/adr/0005）。 */
    void like(UUID postId, CurrentUser viewer);

    /** 冪等。いいねしていなくても例外を投げない。 */
    void unlike(UUID postId, CurrentUser viewer);
}
