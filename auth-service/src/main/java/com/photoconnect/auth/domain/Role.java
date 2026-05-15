package com.photoconnect.auth.domain;

/**
 * The two user types in PhotoConnect's MVP.
 *
 * <p>Stored in the DB as the enum name ({@code @Enumerated(EnumType.STRING)}
 * — never ordinal, which would silently corrupt data if we reorder).</p>
 *
 * <p>Mirrored as a string claim in the JWT payload. Spring Security uses
 * {@code "ROLE_" + name()} as the authority — so {@code @PreAuthorize}
 * expressions read like {@code hasRole('PHOTOGRAPHER')}.</p>
 */
public enum Role {
    PHOTOGRAPHER,
    CUSTOMER
}
