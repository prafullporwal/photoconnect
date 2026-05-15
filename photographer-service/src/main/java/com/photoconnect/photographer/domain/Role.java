package com.photoconnect.photographer.domain;

/**
 * User roles forwarded by api-gateway as {@code X-User-Role}.
 *
 * <p>Must stay in sync with {@code com.photoconnect.auth.domain.Role}.
 * In Phase 2, both services would share this enum via a common library.</p>
 */
public enum Role {
    PHOTOGRAPHER,
    CUSTOMER
}
