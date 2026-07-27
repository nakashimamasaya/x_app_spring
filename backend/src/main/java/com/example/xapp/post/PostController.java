package com.example.xapp.post;

import com.example.xapp.common.AuthenticatedUser;
import com.example.xapp.common.CurrentUser;
import com.example.xapp.post.dto.CreatePostRequest;
import com.example.xapp.post.dto.PostResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * api/openapi.yaml の posts タグに対応する。
 *
 * <p>{@code postId} が UUID として解釈できない場合、Spring の型変換が失敗して 400 になる
 * （存在しない UUID の 404 とは区別される）。
 */
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @AuthenticatedUser CurrentUser author, @Valid @RequestBody CreatePostRequest request) {
        return postService.create(author, request);
    }

    @GetMapping("/{postId}")
    public PostResponse get(@PathVariable UUID postId, @AuthenticatedUser CurrentUser viewer) {
        return postService.get(postId, viewer);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID postId, @AuthenticatedUser CurrentUser viewer) {
        postService.delete(postId, viewer);
    }

    @PostMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@PathVariable UUID postId, @AuthenticatedUser CurrentUser viewer) {
        postService.like(postId, viewer);
    }

    @DeleteMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@PathVariable UUID postId, @AuthenticatedUser CurrentUser viewer) {
        postService.unlike(postId, viewer);
    }
}
