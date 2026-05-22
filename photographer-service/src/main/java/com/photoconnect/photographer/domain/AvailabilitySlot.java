package com.photoconnect.photographer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single day the photographer has marked as available for bookings.
 *
 * <h2>Why no {@code @Version}?</h2>
 * <p>Availability slots are immutable after creation in the MVP — the only
 * mutations are <em>insert</em> (mark a day available) and <em>delete</em>
 * (un-mark). With no field updates there's no lost-update concern, so
 * optimistic locking would be ceremony for its own sake. {@link Favorite}'s
 * comment in customer-service applies for the same reason.</p>
 *
 * <h2>Date semantics</h2>
 * <p>{@code availableDate} is a calendar date, NOT a timestamp — the column is
 * SQL {@code DATE}, no timezone. We treat all dates as the photographer's
 * local interpretation; the UI does not convert. When two photographers in
 * different timezones each mark "May 24", they're each talking about their
 * own May 24, and customers see them that way.</p>
 */
@Entity
@Table(name = "availability_slots")
@Getter
@Setter
@NoArgsConstructor
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "photographer_profile_id", nullable = false)
    private UUID photographerProfileId;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    /** Free-form note shown to customers — e.g. "morning only", "outdoor preferred". */
    @Column(length = 200)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        createdAt = Instant.now();
    }
}
