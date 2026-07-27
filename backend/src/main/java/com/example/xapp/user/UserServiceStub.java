package com.example.xapp.user;

import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.post.dto.PostResponse;
import com.example.xapp.user.dto.UpdateProfileRequest;
import com.example.xapp.user.dto.UserProfileResponse;
import com.example.xapp.user.dto.UserResponse;
import org.springframework.stereotype.Service;

/**
 * UserService の未実装スタブ。フェーズ3 で本実装に置き換える。
 *
 * <p>テストが「仕様どおりに落ちる」ことを確認できるよう、コンテキストは起動させたうえで
 * 呼び出し時に UnsupportedOperationException を投げる。
 */
@Service
public class UserServiceStub implements UserService {

    @Override
    public UserResponse getCurrentUser(CurrentUser viewer) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public UserResponse updateProfile(CurrentUser viewer, UpdateProfileRequest request) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public UserProfileResponse getProfile(String username, CurrentUser viewer) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public CursorPage<PostResponse> listUserPosts(String username, CurrentUser viewer, String cursor, int limit) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public void follow(CurrentUser viewer, String targetUsername) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public void unfollow(CurrentUser viewer, String targetUsername) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public CursorPage<UserResponse> listFollowers(String username, String cursor, int limit) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public CursorPage<UserResponse> listFollowing(String username, String cursor, int limit) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

}
