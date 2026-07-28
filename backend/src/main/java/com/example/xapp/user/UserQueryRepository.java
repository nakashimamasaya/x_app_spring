package com.example.xapp.user;

import com.example.xapp.common.CursorCodec;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.user.dto.UserResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Spring Data のメソッド名クエリでは書けない SQL を集約する。
 *
 * <p>カウンタは非正規化せず都度集計する（docs/adr/0006）。3 つの COUNT を
 * 別々のクエリにすると N+1 になるので、1 クエリでまとめて取る。
 */
@Repository
public class UserQueryRepository {

    public record Counts(long postCount, long followerCount, long followingCount) {}

    private static final RowMapper<UserResponse> USER_MAPPER =
            (rs, rowNum) ->
                    new UserResponse(
                            rs.getObject("id", UUID.class),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("bio"),
                            rs.getTimestamp("created_at").toInstant());

    private final NamedParameterJdbcTemplate jdbc;

    public UserQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 投稿数・フォロワー数・フォロー中数を 1 クエリで取る。 */
    public Counts countsOf(UUID userId) {
        String sql =
                """
                SELECT (SELECT count(*) FROM posts p
                        WHERE p.author_id = :id AND p.deleted_at IS NULL) AS post_count,
                       (SELECT count(*) FROM follows f WHERE f.followee_id = :id) AS follower_count,
                       (SELECT count(*) FROM follows f WHERE f.follower_id = :id) AS following_count
                """;
        return jdbc.queryForObject(
                sql,
                new MapSqlParameterSource("id", userId),
                (rs, n) ->
                        new Counts(
                                rs.getLong("post_count"),
                                rs.getLong("follower_count"),
                                rs.getLong("following_count")));
    }

    @SuppressWarnings("unused")
    public void updateProfile(UUID userId, String displayName, String bio) {
        // 省略されたフィールドは変更しない。COALESCE で null を現在値に落とす
        jdbc.update(
                """
                UPDATE users
                SET display_name = COALESCE(:displayName, display_name),
                    bio          = COALESCE(:bio, bio),
                    updated_at   = now()
                WHERE id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", userId)
                        .addValue("displayName", displayName)
                        .addValue("bio", bio));
    }

    /**
     * 冪等なフォロー（docs/adr/0005）。
     *
     * <p>{@code ON CONFLICT DO NOTHING} により、複合主キー違反が例外にならない。
     * 競合状態でも 204 を返せる。
     */
    public void insertFollowIgnoringConflict(UUID followerId, UUID followeeId) {
        jdbc.update(
                """
                INSERT INTO follows (follower_id, followee_id)
                VALUES (:followerId, :followeeId)
                ON CONFLICT DO NOTHING
                """,
                new MapSqlParameterSource()
                        .addValue("followerId", followerId)
                        .addValue("followeeId", followeeId));
    }

    /** 冪等。存在しない行を消しても影響なし。 */
    public void deleteFollow(UUID followerId, UUID followeeId) {
        jdbc.update(
                "DELETE FROM follows WHERE follower_id = :followerId AND followee_id = :followeeId",
                new MapSqlParameterSource()
                        .addValue("followerId", followerId)
                        .addValue("followeeId", followeeId));
    }

    /** フォロワー一覧。カーソルは users.id を基準にする。 */
    public CursorPage<UserResponse> followersOf(UUID userId, String cursor, int limit) {
        return pageOfUsers(
                """
                SELECT u.id, u.username, u.display_name, u.bio, u.created_at
                FROM follows f JOIN users u ON u.id = f.follower_id
                WHERE f.followee_id = :id
                """,
                userId, cursor, limit);
    }

    public CursorPage<UserResponse> followingOf(UUID userId, String cursor, int limit) {
        return pageOfUsers(
                """
                SELECT u.id, u.username, u.display_name, u.bio, u.created_at
                FROM follows f JOIN users u ON u.id = f.followee_id
                WHERE f.follower_id = :id
                """,
                userId, cursor, limit);
    }

    private CursorPage<UserResponse> pageOfUsers(String baseSql, UUID userId, String cursor, int limit) {
        UUID cursorId = CursorCodec.decode(cursor);
        String sql = baseSql
                + (cursorId == null ? "" : " AND u.id < :cursorId")
                + " ORDER BY u.id DESC LIMIT :limit";

        var params =
                new MapSqlParameterSource().addValue("id", userId).addValue("limit", limit + 1);
        if (cursorId != null) {
            params.addValue("cursorId", cursorId);
        }

        List<UserResponse> rows = new ArrayList<>(jdbc.query(sql, params, USER_MAPPER));
        String nextCursor = null;
        if (rows.size() > limit) {
            rows.removeLast();
            nextCursor = CursorCodec.encode(rows.getLast().id());
        }
        return new CursorPage<>(rows, nextCursor);
    }
}
