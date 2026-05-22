package com.photoconnect.customer.client;

import com.photoconnect.customer.config.ServiceClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

/**
 * Caches a service-to-service JWT minted by auth-service and refreshes it
 * just before expiry.
 *
 * <h2>Why cache?</h2>
 * <p>Service tokens have a ~60s TTL — but a single inbound user request can
 * trigger multiple downstream calls. Minting on every Feign call would:</p>
 * <ul>
 *   <li>turn a 1-hop request into a 2-hop request (auth + target),</li>
 *   <li>load auth-service with BCrypt verifications, which are intentionally slow.</li>
 * </ul>
 * <p>Caching the token for its lifetime and refreshing slightly before expiry
 * keeps the steady-state cost at one auth-service call per (TTL − refresh-skew).</p>
 *
 * <h2>Thread safety</h2>
 * <p>Token mint+swap is guarded by {@code synchronized}. Reads of a non-expiring
 * token are uncontended (volatile {@code cached} reference). When several
 * threads race past the expiry check at the same time, only one of them
 * actually hits auth-service — the others block briefly and then read the
 * freshly-minted token. Lost wakeups are not possible because we hold the
 * lock across the read-check-mint-store cycle.</p>
 */
@Slf4j
@Component
public class ServiceTokenClient {

    private final ServiceClientProperties properties;
    private final RestClient restClient;

    private volatile CachedToken cached;

    public ServiceTokenClient(ServiceClientProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.authServiceUrl())
                .build();
    }

    /**
     * Get a usable token. Returns the cached one if it still has more than
     * {@code refreshSkew} left, otherwise mints a fresh one synchronously.
     */
    public String getToken() {
        CachedToken c = cached;
        if (c != null && Instant.now().isBefore(c.refreshAt())) {
            return c.token();
        }
        return mintAndCache();
    }

    private synchronized String mintAndCache() {
        // Double-check inside the lock: a concurrent caller may have already
        // refreshed while we were waiting.
        CachedToken c = cached;
        if (c != null && Instant.now().isBefore(c.refreshAt())) {
            return c.token();
        }

        log.debug("Minting new service token for clientId={}", properties.clientId());
        ServiceTokenResponse resp;
        try {
            resp = restClient.post()
                    .uri("/api/v1/auth/service-token")
                    .body(new ServiceTokenRequest(properties.clientId(), properties.clientSecret()))
                    .retrieve()
                    .body(ServiceTokenResponse.class);
        } catch (RestClientException ex) {
            log.error("Failed to mint service token from {}: {}",
                    properties.authServiceUrl(), ex.getMessage());
            throw ex;
        }
        if (resp == null || resp.accessToken() == null) {
            throw new IllegalStateException("auth-service returned an empty service-token response");
        }

        Instant refreshAt = resp.expiresAt().minus(properties.refreshSkew());
        this.cached = new CachedToken(resp.accessToken(), resp.expiresAt(), refreshAt);
        log.info("Cached service token expiresAt={} refreshAt={}", resp.expiresAt(), refreshAt);
        return resp.accessToken();
    }

    /** Cached token plus the wall-clock instant at which we should mint a fresh one. */
    private record CachedToken(String token, Instant expiresAt, Instant refreshAt) {}

    /** Request payload — must match the shape of auth-service's {@code ServiceTokenRequest}. */
    record ServiceTokenRequest(String clientId, String clientSecret) {}

    /** Response payload — must match the shape of auth-service's {@code ServiceTokenResponse}. */
    record ServiceTokenResponse(String accessToken, Instant expiresAt, String scope) {}
}
