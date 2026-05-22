package com.photoconnect.auth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

/**
 * Registered service-to-service clients allowed to mint internal tokens.
 *
 * <p>Each entry binds a {@code clientId} (used as the JWT {@code sub} on issued
 * tokens) to a BCrypt-hashed secret plus a comma-separated list of granted
 * scopes. Lives in {@code config-repo/auth-service.properties} under
 * {@code auth.service-clients.*}. In Phase 2 these move to a DB table or
 * AWS Secrets Manager — the surface of {@link Client} stays the same.</p>
 *
 * <p>Generate a secret hash with:
 * <pre>{@code
 *   htpasswd -nbBC 12 "" "my-secret" | cut -d':' -f2
 * }</pre>
 * or any BCrypt utility configured to strength 12 (matches the password
 * encoder used for user credentials).</p>
 *
 * @param tokenTtl    lifetime of minted service tokens (default 60s)
 * @param clients     map of {@code clientId -> Client config}
 */
@Validated
@ConfigurationProperties(prefix = "auth.service-clients")
public record ServiceClientsProperties(
        @DefaultValue("PT60S") Duration tokenTtl,
        @DefaultValue Map<String, Client> clients
) {

    /**
     * @param secretHash  BCrypt hash of the shared secret (strength 12)
     * @param scope       comma-separated list of permitted scopes (e.g. {@code "photographer-read"})
     */
    public record Client(
            @NotBlank String secretHash,
            @NotBlank String scope
    ) {}
}
