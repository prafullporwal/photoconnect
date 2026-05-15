package com.photoconnect.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Customer profile.
 *
 * <p>Same pattern as {@code PhotographerProfile} in the sister service —
 * {@code userId} links back to auth-service by UUID (no FK between services).</p>
 *
 * <h2>MySQL specifics</h2>
 * <ul>
 *   <li>UUID stored as {@code VARCHAR(36)} (column-defined via {@code columnDefinition})
 *       — readable in workbench queries and avoids byte-order quirks of {@code BINARY(16)}.</li>
 *   <li>{@code DATETIME(6)} instead of {@code TIMESTAMPTZ} — MySQL's
 *       {@code DATETIME} has no timezone; we always write UTC {@link Instant}.</li>
 * </ul>
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 200)
    private String location;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact_method", nullable = false, length = 20)
    private ContactMethod preferredContactMethod = ContactMethod.EMAIL;

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
