package com.example.xapp.user;

import com.example.xapp.common.AuthenticatedUser;
import com.example.xapp.common.CurrentUser;
import com.example.xapp.common.dto.CursorPage;
import com.example.xapp.post.dto.PostResponse;
import com.example.xapp.user.dto.UpdateProfileRequest;
import com.example.xapp.user.dto.UserProfileResponse;
import com.example.xapp.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** api/openapi.yaml の users タグに対応する。 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticatedUser CurrentUser viewer) {
        return userService.getCurrentUser(viewer);
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(
            @AuthenticatedUser CurrentUser viewer, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(viewer, request);
    }

    @GetMapping("/{username}")
    public UserProfileResponse getProfile(
            @PathVariable String username, @AuthenticatedUser CurrentUser viewer) {
        return userService.getProfile(username, viewer);
    }

    @GetMapping("/{username}/posts")
    public CursorPage<PostResponse> listUserPosts(
            @PathVariable String username,
            @AuthenticatedUser CurrentUser viewer,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return userService.listUserPosts(username, viewer, cursor, limit);
    }

    /** 冪等。既にフォロー済みでも 204（docs/adr/0005）。 */
    @PostMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@AuthenticatedUser CurrentUser viewer, @PathVariable String username) {
        userService.follow(viewer, username);
    }

    /** 冪等。フォローしていなくても 204。 */
    @DeleteMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@AuthenticatedUser CurrentUser viewer, @PathVariable String username) {
        userService.unfollow(viewer, username);
    }

    @GetMapping("/{username}/followers")
    public CursorPage<UserResponse> listFollowers(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return userService.listFollowers(username, cursor, limit);
    }

    @GetMapping("/{username}/following")
    public CursorPage<UserResponse> listFollowing(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return userService.listFollowing(username, cursor, limit);
    }
}
