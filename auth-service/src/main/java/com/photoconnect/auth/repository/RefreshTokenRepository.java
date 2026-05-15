package com.photoconnect.auth.repository;

import com.photoconnect.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenId(String tokenId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.tokenId = :tokenId")
    int revokeByTokenId(String tokenId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    int revokeAllForUser(UUID userId);

    /** Housekeeping for an eventual scheduled cleanup job. */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);
}
