package com.photoconnect.photographer.repository;

import com.photoconnect.photographer.domain.PhotographerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PhotographerProfile}.
 *
 * <p>Spring Data generates all implementations at startup from the method
 * names — no SQL needed for these simple queries.</p>
 */
public interface PhotographerProfileRepository extends JpaRepository<PhotographerProfile, UUID> {

    /**
     * Find a profile by the auth-service user UUID.
     * Used for all {@code /me} operations where we know who is logged in but
     * not the profile's own PK.
     */
    Optional<PhotographerProfile> findByUserId(UUID userId);

    /**
     * Check existence by user UUID — faster than {@link #findByUserId}
     * when we only need to know if a profile exists (e.g. duplicate check).
     */
    boolean existsByUserId(UUID userId);

    /**
     * Browse endpoint: only photographers who are open for new bookings.
     * In Phase 2, this would accept paging, location filters, specialty filters, etc.
     */
    List<PhotographerProfile> findAllByAvailableTrue();
}
