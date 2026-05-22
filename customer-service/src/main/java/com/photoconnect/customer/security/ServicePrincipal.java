package com.photoconnect.customer.security;

/**
 * The "who" behind a service-to-service request. Mirrors {@link GatewayPrincipal}
 * for user requests.
 *
 * @param clientId  the calling service's id (e.g. {@code "reviews-service"})
 * @param scope     comma-separated scopes granted on this token
 */
public record ServicePrincipal(String clientId, String scope) {}
