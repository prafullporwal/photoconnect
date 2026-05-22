package com.photoconnect.reviews.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer's review of a photographer.
 *
 * <h2>Why both photographer ids?</h2>
 * <p>{@code photographerProfileId} drives deep-linking back to the marketplace
 * profile page; {@code photographerUserId} is the auth identity that survives
 * profile changes (a photographer could delete and recreate their profile and
 * still own these reviews). Denormalised at write time from the photographer-
 * service Feign call, same pattern as customer-service's Inquiry.</p>
 *
 * <h2>Why store inquiryId?</h2>
 * <p>It's the audit trail: "this review exists because of inquiry X, which
 * was COMPLETED at the time we let the customer write it." Useful for ops
 * investigations and indispensable when Phase 2 swaps inquiries for proper
 * Booking rows.</p>
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /** auth-service userId of the customer who wrote this review. */
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** PhotographerProfile PK in photographer-service. */
    @Column(name = "photographer_profile_id", nullable = false)
    private UUID photographerProfileId;

    /** auth-service userId of the photographer being reviewed. */
    @Column(name = "photographer_user_id", nullable = false)
    private UUID photographerUserId;

    /** The completed inquiry/booking that authorised this review. */
    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    /** Integer 1..5. SMALLINT in the DB; CHECK constraint mirrors the DTO bounds. */
    @Column(nullable = false)
    private short rating;

    /** Optional free-text body; nullable on purpose so customers can rate without writing. */
    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }
}
