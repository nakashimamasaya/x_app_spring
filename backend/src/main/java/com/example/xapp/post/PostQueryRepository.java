package com.example.xapp.post;

import com.example.xapp.common.CursorCodec;
import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.post.dto.PostResponse;
import com.example.xapp.user.dto.AuthorSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 投稿一覧を「投稿・著者・いいね数・閲覧者のいいね有無」まとめて 1 クエリで取る。
 *
 * <p>カウンタを非正規化しない方針（docs/adr/0006）なので、一覧で投稿ごとに COUNT を
 * 撃つと即座に N+1 で破綻する。それを避けるための相関サブクエリをここに集約している。
 *
 * <p>Spring Data のメソッド名クエリでは表現できないため手書きする。
 * <strong>ローカル LLM に委譲しない領域。</strong>
 */
@Repository
public class PostQueryRepository {

    /**
     * {@code likedByMe} は閲覧者が未認証なら NULL を返す。
     * {@code false}（いいねしていない）と「不明」を区別するため（api/openapi.yaml）。
     */
    private static final String SELECT =
            """
            SELECT p.id            AS post_id,
                   p.body          AS body,
                   p.created_at    AS created_at,
                   u.id            AS author_id,
                   u.username      AS author_username,
                   u.display_name  AS author_display_name,
                   (SELECT count(*) FROM likes l WHERE l.post_id = p.id) AS like_count,
                   CASE WHEN CAST(:viewerId AS uuid) IS NULL THEN NULL
                        ELSE EXISTS (SELECT 1 FROM likes lm
                                     WHERE lm.post_id = p.id AND lm.user_id = CAST(:viewerId AS uuid))
                   END             AS liked_by_me
            FROM posts p
            JOIN users u ON u.id = p.author_id
            WHERE p.deleted_at IS NULL
            """;

    private static final RowMapper<PostResponse> MAPPER =
            (rs, rowNum) -> {
                Boolean likedByMe = rs.getObject("liked_by_me", Boolean.class);
                return new PostResponse(
                        rs.getObject("post_id", UUID.class),
                        new AuthorSummary(
                                rs.getObject("author_id", UUID.class),
                                rs.getString("author_username"),
                                rs.getString("author_display_name")),
                        rs.getString("body"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getLong("like_count"),
                        likedByMe);
            };

    private final NamedParameterJdbcTemplate jdbc;

    public PostQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 単一投稿。削除済みは存在しないものとして扱う（INV-5）。 */
    public PostResponse findById(UUID postId, CurrentUser viewer) {
        var params =
                new MapSqlParameterSource()
                        .addValue("viewerId", viewerId(viewer))
                        .addValue("postId", postId);
        var rows = jdbc.query(SELECT + " AND p.id = :postId", params, MAPPER);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public CursorPage<PostResponse> byAuthor(UUID authorId, CurrentUser viewer, String cursor, int limit) {
        return page(" AND p.author_id = :authorId",
                new MapSqlParameterSource().addValue("authorId", authorId), viewer, cursor, limit);
    }

    /** ホームタイムライン: 自分自身とフォロー中ユーザーの投稿。 */
    public CursorPage<PostResponse> homeOf(UUID viewerId, CurrentUser viewer, String cursor, int limit) {
        String condition =
                """
                 AND (p.author_id = :selfId
                      OR p.author_id IN (SELECT f.followee_id FROM follows f
                                         WHERE f.follower_id = :selfId))
                """;
        return page(condition, new MapSqlParameterSource().addValue("selfId", viewerId),
                viewer, cursor, limit);
    }

    /** 公開タイムライン: 全ユーザーの投稿。 */
    public CursorPage<PostResponse> publicTimeline(CurrentUser viewer, String cursor, int limit) {
        return page("", new MapSqlParameterSource(), viewer, cursor, limit);
    }

    /**
     * limit + 1 件取得して次ページの有無を判定する。COUNT(*) は発行しない（docs/adr/0003）。
     *
     * <p>カーソルの条件は SQL を組み立て分ける。null の UUID をバインドすると
     * PostgreSQL が型を推論できず落ちるため。
     */
    private CursorPage<PostResponse> page(
            String condition, MapSqlParameterSource params, CurrentUser viewer, String cursor, int limit) {

        UUID cursorId = CursorCodec.decode(cursor);
        String sql = SELECT + condition
                + (cursorId == null ? "" : " AND p.id < :cursorId")
                + " ORDER BY p.id DESC LIMIT :limit";

        params.addValue("viewerId", viewerId(viewer));
        params.addValue("limit", limit + 1);
        if (cursorId != null) {
            params.addValue("cursorId", cursorId);
        }

        List<PostResponse> rows = new ArrayList<>(jdbc.query(sql, params, MAPPER));

        String nextCursor = null;
        if (rows.size() > limit) {
            rows.removeLast();
            nextCursor = CursorCodec.encode(rows.getLast().id());
        }
        return new CursorPage<>(rows, nextCursor);
    }

    private static UUID viewerId(CurrentUser viewer) {
        return viewer == null ? null : viewer.id();
    }
}
