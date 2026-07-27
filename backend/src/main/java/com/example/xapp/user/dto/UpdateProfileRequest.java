package com.example.xapp.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 省略したフィールドは変更されない。両方 null なら 400（最低 1 つは指定が必要）。
 *
 * <p>username と email は変更できないため、ここに定義しない。
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 50) String displayName, @Size(max = 160) String bio) {

    public boolean isEmpty() {
        return displayName == null && bio == null;
    }
}
