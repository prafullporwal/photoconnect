package com.photoconnect.auth.service;

import com.photoconnect.auth.config.ServiceClientsProperties;
import com.photoconnect.auth.dto.ServiceTokenRequest;
import com.photoconnect.auth.dto.ServiceTokenResponse;
import com.photoconnect.auth.exception.InvalidServiceClientException;
import com.photoconnect.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Mints short-lived service-to-service tokens via OAuth2-style
 * client-credentials. The registry of allowed clients lives in
 * {@link ServiceClientsProperties} — backed by config-repo today, by a DB
 * table or AWS Secrets Manager in Phase 2.
 *
 * <p>The flow is deliberately tiny:</p>
 * <ol>
 *   <li>Look up the client by id. Absent → fail.</li>
 *   <li>BCrypt-compare the presented secret. Mismatch → fail.</li>
 *   <li>Ask {@link JwtService} to mint a {@code typ=service} JWT with the
 *       registered scope and the configured TTL.</li>
 * </ol>
 *
 * <p>Both failure modes throw the SAME exception with the same message — see
 * {@link InvalidServiceClientException} for why we don't differentiate.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTokenService {

    private final ServiceClientsProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ServiceTokenResponse issue(ServiceTokenRequest request) {
        ServiceClientsProperties.Client client = properties.clients() == null
                ? null
                : properties.clients().get(request.clientId());
        if (client == null) {
            log.warn("Service-token request rejected: unknown clientId={}", request.clientId());
            throw new InvalidServiceClientException();
        }
        if (!passwordEncoder.matches(request.clientSecret(), client.secretHash())) {
            log.warn("Service-token request rejected: bad secret for clientId={}", request.clientId());
            throw new InvalidServiceClientException();
        }

        JwtService.IssuedToken issued = jwtService.generateServiceToken(
                request.clientId(), client.scope(), properties.tokenTtl());

        log.info("Minted service token clientId={} scope={} ttl={} jti={}",
                request.clientId(), client.scope(), properties.tokenTtl(), issued.jti());

        return new ServiceTokenResponse(issued.token(), issued.expiresAt(), client.scope());
    }
}
