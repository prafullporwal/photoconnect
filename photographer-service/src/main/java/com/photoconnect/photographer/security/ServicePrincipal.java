package com.photoconnect.photographer.security;

/**
 * The "who" behind a service-to-service request. Mirrors {@link GatewayPrincipal}
 * for user requests: a small immutable record that gets stored as the
 * {@code Authentication} principal, available via {@code @AuthenticationPrincipal}
 * in controllers (rare — usually the request just needs to be authorized, not
 * identified).
 *
 * @param clientId  the calling service's id (e.g. {@code "customer-service"})
 * @param scope     comma-separated scopes granted on this token
 */
public record ServicePrincipal(String clientId, String scope) {}
