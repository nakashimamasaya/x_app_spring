package com.example.xapp.post;

import com.example.xapp.post.LikeEntity.LikeId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<LikeEntity, LikeId> {

    boolean existsByIdUserIdAndIdPostId(UUID userId, UUID postId);

    long countByIdPostId(UUID postId);
}
