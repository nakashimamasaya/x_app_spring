package com.example.xapp.post;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    /** 削除済みは存在しないものとして扱う（INV-5）。 */
    Optional<PostEntity> findByIdAndDeletedAtIsNull(UUID id);

    long countByAuthorIdAndDeletedAtIsNull(UUID authorId);
}
