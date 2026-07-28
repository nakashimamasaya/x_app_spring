package com.example.xapp.user;

import com.example.xapp.common.AppProperties;
import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.PageLimit;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.common.exception.EmptyUpdateException;
import com.example.xapp.common.exception.SelfFollowException;
import com.example.xapp.common.exception.UserNotFoundException;
import com.example.xapp.post.PostQueryRepository;
import com.example.xapp.post.dto.PostResponse;
import com.example.xapp.user.dto.UpdateProfileRequest;
import com.example.xapp.user.dto.UserProfileResponse;
import com.example.xapp.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final FollowRepository follows;
    private final PostQueryRepository postQuery;
    private final UserQueryRepository userQuery;
    private final AppProperties props;

    public UserServiceImpl(
            UserRepository users,
            FollowRepository follows,
            PostQueryRepository postQuery,
            UserQueryRepository userQuery,
            AppProperties props) {
        this.users = users;
        this.follows = follows;
        this.postQuery = postQuery;
        this.userQuery = userQuery;
        this.props = props;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(CurrentUser viewer) {
        return toResponse(requireById(viewer.id()));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(CurrentUser viewer, UpdateProfileRequest request) {
        if (request.isEmpty()) {
            throw new EmptyUpdateException();
        }
        userQuery.updateProfile(viewer.id(), request.displayName(), request.bio());
        return toResponse(requireById(viewer.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username, CurrentUser viewer) {
        UserEntity user = requireByUsername(username);

        // 未認証なら isFollowing は false ではなく null（不明）にする
        Boolean isFollowing =
                viewer == null
                        ? null
                        : follows.existsByIdFollowerIdAndIdFolloweeId(viewer.id(), user.getId());

        var counts = userQuery.countsOf(user.getId());
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getCreatedAt(),
                counts.postCount(),
                counts.followerCount(),
                counts.followingCount(),
                isFollowing);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> listUserPosts(
            String username, CurrentUser viewer, String cursor, int limit) {
        UserEntity user = requireByUsername(username);
        return postQuery.byAuthor(user.getId(), viewer, cursor, PageLimit.validate(limit, props));
    }

    @Override
    @Transactional
    public void follow(CurrentUser viewer, String targetUsername) {
        UserEntity target = requireByUsername(targetUsername);
        if (target.getId().equals(viewer.id())) {
            // 「既にその状態」ではなく論理的にあり得ない要求なので、冪等にせず 400
            throw new SelfFollowException();
        }
        // 冪等（docs/adr/0005）。複合主キー違反はエラーにせず握りつぶす。
        // 競合状態でも 204 を返せるようにするため
        userQuery.insertFollowIgnoringConflict(viewer.id(), target.getId());
    }

    @Override
    @Transactional
    public void unfollow(CurrentUser viewer, String targetUsername) {
        UserEntity target = requireByUsername(targetUsername);
        // 冪等。フォローしていなくても例外を投げない
        userQuery.deleteFollow(viewer.id(), target.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserResponse> listFollowers(String username, String cursor, int limit) {
        return userQuery.followersOf(
                requireByUsername(username).getId(), cursor, PageLimit.validate(limit, props));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserResponse> listFollowing(String username, String cursor, int limit) {
        return userQuery.followingOf(
                requireByUsername(username).getId(), cursor, PageLimit.validate(limit, props));
    }

    private UserEntity requireByUsername(String username) {
        return users.findByUsername(username).orElseThrow(UserNotFoundException::new);
    }

    private UserEntity requireById(UUID id) {
        return users.findById(id).orElseThrow(UserNotFoundException::new);
    }

    static UserResponse toResponse(UserEntity u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getDisplayName(), u.getBio(), u.getCreatedAt());
    }
}
