package com.photoconnect.reviews.client;

import com.photoconnect.reviews.config.ServiceClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

/**
 * Caches a service-to-service JWT minted by auth-service and refreshes it
 * just before expiry. Identical to customer-service's variant — kept duplicated
 * on purpose so each service owns its outbound-auth setup with no shared-jar
 * coupling.
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

    private record CachedToken(String token, Instant expiresAt, Instant refreshAt) {}

    record ServiceTokenRequest(String clientId, String clientSecret) {}

    record ServiceTokenResponse(String accessToken, Instant expiresAt, String scope) {}
}
