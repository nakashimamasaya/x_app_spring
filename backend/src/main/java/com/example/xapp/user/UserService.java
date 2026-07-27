package com.example.xapp.user;

import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.post.dto.PostResponse;
import com.example.xapp.user.dto.UpdateProfileRequest;
import com.example.xapp.user.dto.UserProfileResponse;
import com.example.xapp.user.dto.UserResponse;

/**
 * プロフィールとフォロー関係のユースケース。実装はフェーズ3。
 *
 * <p>{@code viewer} は未認証なら {@code null}。その場合 {@code isFollowing} は
 * {@code false} ではなく {@code null}（不明）になる。
 */
public interface UserService {

    UserResponse getCurrentUser(CurrentUser viewer);

    UserResponse updateProfile(CurrentUser viewer, UpdateProfileRequest request);

    UserProfileResponse getProfile(String username, CurrentUser viewer);

    CursorPage<PostResponse> listUserPosts(String username, CurrentUser viewer, String cursor, int limit);

    /** 冪等。既にフォロー済みでも例外を投げない（docs/adr/0005）。自己フォローは例外。 */
    void follow(CurrentUser viewer, String targetUsername);

    /** 冪等。フォローしていなくても例外を投げない。 */
    void unfollow(CurrentUser viewer, String targetUsername);

    CursorPage<UserResponse> listFollowers(String username, String cursor, int limit);

    CursorPage<UserResponse> listFollowing(String username, String cursor, int limit);
}
