package com.photoconnect.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A platform user — photographer OR customer.
 *
 * <p>Owns its own table in {@code auth_db}. Email is unique among non-deleted
 * users (enforced by a partial unique index in Flyway V1).</p>
 *
 * <p>Soft delete: setting {@code deletedAt} non-null hides the user from
 * default queries (we'll add a JPA filter or just check this in repos).</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    /** BCrypt hash; NEVER the plaintext password. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** Soft-delete marker. NULL = active. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;
}
