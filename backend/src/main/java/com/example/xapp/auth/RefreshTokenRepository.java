package com.example.xapp.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /** 失効済みトークンの再提示を検知するため、revoked_at の状態に関わらず引く（INV-10）。 */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}
