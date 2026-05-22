package com.photoconnect.photographer.repository;

import com.photoconnect.photographer.domain.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    /**
     * "Show me this photographer's calendar, earliest first." Drives both the
     * owner's editor and the customer-facing date picker.
     */
    List<AvailabilitySlot> findByPhotographerProfileIdOrderByAvailableDateAsc(UUID photographerProfileId);

    /**
     * Used by customer-service's pre-inquiry check (via Feign) to confirm the
     * customer selected a still-available date.
     */
    boolean existsByPhotographerProfileIdAndAvailableDate(UUID photographerProfileId, LocalDate date);

    /**
     * Owner-scoped delete: the slot only goes away when the caller owns it.
     * Returns the number of rows actually removed so the service distinguishes
     * "deleted it" from "nothing there to delete" (both are 204 in REST terms,
     * but we log the difference for diagnostics).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AvailabilitySlot s WHERE s.id = :id " +
            "AND s.photographerProfileId = :photographerProfileId")
    int deleteByOwnedId(UUID photographerProfileId, UUID id);

    /** Bulk wipe — used when the photographer clears their whole calendar at once. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AvailabilitySlot s WHERE s.photographerProfileId = :photographerProfileId")
    int deleteAllForProfile(UUID photographerProfileId);
}
