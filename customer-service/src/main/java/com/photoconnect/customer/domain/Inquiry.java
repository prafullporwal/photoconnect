package com.photoconnect.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A customer's inquiry to a specific photographer about a potential booking.
 *
 * <h2>Why store BOTH photographer IDs?</h2>
 * <p>{@code photographerProfileId} is the PhotographerProfile PK (used for
 * deep-linking back to the profile page). {@code photographerUserId} is the
 * underlying auth-service user UUID — needed so the photographer can later
 * query "show me inquiries received by me" (the X-User-Id forwarded by the
 * gateway is the user UUID, not the profile UUID).</p>
 *
 * <p>This is the standard pattern for cross-service references: store the
 * entity's own ID AND the auth identity, denormalised at write time when we
 * can still resolve the relationship cheaply via Feign.</p>
 */
@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private UUID id;

    /** auth-service userId of the customer who created this inquiry. */
    @Column(name = "customer_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID customerId;

    /** PhotographerProfile PK in photographer-service. */
    @Column(name = "photographer_profile_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID photographerProfileId;

    /** auth-service userId of the photographer — used to route "received" queries. */
    @Column(name = "photographer_user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID photographerUserId;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(length = 200)
    private String location;

    @Column(precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.NEW;

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
        if (status == null) status = InquiryStatus.NEW;
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }
}
