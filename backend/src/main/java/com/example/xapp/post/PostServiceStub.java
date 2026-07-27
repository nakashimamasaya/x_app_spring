package com.example.xapp.post;

import com.example.xapp.common.CurrentUser;
import com.example.xapp.post.dto.CreatePostRequest;
import com.example.xapp.post.dto.PostResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * PostService の未実装スタブ。フェーズ3 で本実装に置き換える。
 *
 * <p>テストが「仕様どおりに落ちる」ことを確認できるよう、コンテキストは起動させたうえで
 * 呼び出し時に UnsupportedOperationException を投げる。
 */
@Service
public class PostServiceStub implements PostService {

    @Override
    public PostResponse create(CurrentUser author, CreatePostRequest request) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public PostResponse get(UUID postId, CurrentUser viewer) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public void delete(UUID postId, CurrentUser viewer) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public void like(UUID postId, CurrentUser viewer) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public void unlike(UUID postId, CurrentUser viewer) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

}
