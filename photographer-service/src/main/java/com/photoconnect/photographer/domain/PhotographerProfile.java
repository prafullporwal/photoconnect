package com.photoconnect.photographer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core domain entity for a photographer's public profile.
 *
 * <h2>Identity</h2>
 * <p>{@code id} is this profile's own PK. {@code userId} is the UUID that
 * auth-service issued when the user registered — the two services are linked
 * by this UUID, not by a foreign key (each service owns its own database).</p>
 *
 * <h2>Specialties — {@code @ElementCollection}</h2>
 * <p>Specialties are a simple list of strings (e.g. {@code "wedding"},
 * {@code "portrait"}). JPA stores them in a sibling table
 * {@code photographer_specialties} via {@code @ElementCollection}. This is
 * lighter than a full {@code @OneToMany} relationship because specialties have
 * no identity of their own — they exist only as part of this profile.</p>
 *
 * <h2>Auditing without {@code @EnableJpaAuditing}</h2>
 * <p>We use {@code @PrePersist} / {@code @PreUpdate} JPA lifecycle callbacks
 * instead of Spring Data's {@code AuditingEntityListener}. This is simpler
 * when we only need timestamps (no {@code createdBy} / {@code updatedBy}),
 * and it avoids wiring up {@code @EnableJpaAuditing} — which would cause the
 * same {@code @WebMvcTest} context-load failure that auth-service ran into.</p>
 *
 * <h2>Optimistic locking</h2>
 * <p>{@code @Version} prevents lost updates: if two requests try to update
 * the same profile concurrently, one will succeed and the other will get an
 * {@code OptimisticLockException}.</p>
 */
@Entity
@Table(name = "photographer_profiles")
@Getter
@Setter
@NoArgsConstructor
public class PhotographerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Links this profile to auth-service's user record.
     * One user can have at most one photographer profile (enforced by UNIQUE constraint).
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    /**
     * Whether this photographer is open to new bookings.
     * Drives the public browse listing — unavailable photographers are hidden.
     */
    @Column(nullable = false)
    private boolean available = true;

    /**
     * Photography specialties (e.g. "wedding", "portrait", "commercial").
     *
     * <p>Stored in {@code photographer_specialties} table. {@code EAGER} fetch is
     * acceptable here because we always display specialties alongside the profile,
     * and the list is typically small (< 10 items).</p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "photographer_specialties",
            joinColumns = @JoinColumn(name = "photographer_id"))
    @Column(name = "specialty", length = 100)
    private List<String> specialties = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking — incremented on every UPDATE by Hibernate. */
    @Version
    private long version;

    // ── Lifecycle callbacks ───────────────────────────────────────────────────

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
