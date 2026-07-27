package com.example.xapp.user;

import com.example.xapp.user.FollowEntity.FollowId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<FollowEntity, FollowId> {

    boolean existsByIdFollowerIdAndIdFolloweeId(UUID followerId, UUID followeeId);

    long countByIdFolloweeId(UUID followeeId);

    long countByIdFollowerId(UUID followerId);
}
