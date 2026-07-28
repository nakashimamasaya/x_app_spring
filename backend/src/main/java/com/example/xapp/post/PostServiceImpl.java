package com.example.xapp.post;

import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.UuidV7;
import com.example.xapp.common.exception.NotPostAuthorException;
import com.example.xapp.common.exception.PostNotFoundException;
import com.example.xapp.post.dto.CreatePostRequest;
import com.example.xapp.post.dto.PostResponse;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostServiceImpl implements PostService {

    private final PostQueryRepository postQuery;
    private final PostRepository posts;
    private final NamedParameterJdbcTemplate jdbc;

    public PostServiceImpl(
            PostQueryRepository postQuery, PostRepository posts, NamedParameterJdbcTemplate jdbc) {
        this.postQuery = postQuery;
        this.posts = posts;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public PostResponse create(CurrentUser author, CreatePostRequest request) {
        String body = PostBody.normalize(request.body());
        UUID id = UuidV7.generate();

        jdbc.update(
                "INSERT INTO posts (id, author_id, body) VALUES (:id, :authorId, :body)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("authorId", author.id())
                        .addValue("body", body));

        return postQuery.findById(id, author);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse get(UUID postId, CurrentUser viewer) {
        PostResponse post = postQuery.findById(postId, viewer);
        if (post == null) {
            // 存在しない場合と削除済みを区別しない（INV-5）
            throw new PostNotFoundException();
        }
        return post;
    }

    @Override
    @Transactional
    public void delete(UUID postId, CurrentUser viewer) {
        var post = posts.findByIdAndDeletedAtIsNull(postId).orElseThrow(PostNotFoundException::new);
        if (!post.getAuthorId().equals(viewer.id())) {
            throw new NotPostAuthorException();
        }
        // 論理削除。物理削除しないのは、いいねなどの関連を辿れる状態を保つため
        jdbc.update(
                "UPDATE posts SET deleted_at = now() WHERE id = :id AND deleted_at IS NULL",
                new MapSqlParameterSource("id", postId));
    }

    @Override
    @Transactional
    public void like(UUID postId, CurrentUser viewer) {
        requireAlivePost(postId);
        // 冪等（docs/adr/0005）。複合主キー違反を握りつぶし、競合状態でも 204 を返す
        jdbc.update(
                """
                INSERT INTO likes (user_id, post_id) VALUES (:userId, :postId)
                ON CONFLICT DO NOTHING
                """,
                params(viewer.id(), postId));
    }

    @Override
    @Transactional
    public void unlike(UUID postId, CurrentUser viewer) {
        requireAlivePost(postId);
        // 冪等。いいねしていない投稿でも例外を投げない
        jdbc.update(
                "DELETE FROM likes WHERE user_id = :userId AND post_id = :postId",
                params(viewer.id(), postId));
    }

    private void requireAlivePost(UUID postId) {
        if (posts.findByIdAndDeletedAtIsNull(postId).isEmpty()) {
            throw new PostNotFoundException();
        }
    }

    private static MapSqlParameterSource params(UUID userId, UUID postId) {
        return new MapSqlParameterSource().addValue("userId", userId).addValue("postId", postId);
    }
}
