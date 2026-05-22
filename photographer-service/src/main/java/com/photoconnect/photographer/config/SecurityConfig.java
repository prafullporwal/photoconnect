package com.photoconnect.photographer.config;

import com.photoconnect.photographer.security.GatewayAuthenticationFilter;
import com.photoconnect.photographer.security.PemKeyLoader;
import com.photoconnect.photographer.security.ServiceTokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;

/**
 * Spring Security configuration for photographer-service.
 *
 * <h2>Two authentication paths</h2>
 * <ol>
 *   <li><b>User requests via the gateway.</b> The gateway has validated the
 *       user JWT and stamped {@code X-User-Id} + {@code X-User-Role}.
 *       {@link GatewayAuthenticationFilter} promotes those headers into the
 *       {@code SecurityContext}.</li>
 *   <li><b>Service-to-service requests via Eureka.</b> Another service hits us
 *       directly with a {@code Bearer} service JWT minted by auth-service.
 *       {@link ServiceTokenAuthenticationFilter} verifies the signature and
 *       installs a {@code ROLE_SERVICE} + {@code SCOPE_*} authentication.</li>
 * </ol>
 *
 * <p>The service-token filter runs FIRST. If it sets the authentication, the
 * gateway-headers filter sees a populated context and skips its own work. If
 * the request has no Bearer header, the service-token filter is a no-op and
 * the gateway-headers filter handles things as before.</p>
 */
@Slf4j
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ServiceJwtProperties serviceJwtProperties;

    /**
     * Public key used to verify service-to-service JWTs. Loaded once at startup;
     * RSA keys don't rotate at runtime in MVP. Phase 2: hot reload via
     * {@code /actuator/refresh}.
     */
    @Bean
    public RSAPublicKey serviceJwtPublicKey() throws Exception {
        Path path = Path.of(serviceJwtProperties.publicKeyPath());
        log.info("Loading service JWT public key from {}", path);
        return PemKeyLoader.loadPublicKey(path);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    GatewayAuthenticationFilter gatewayFilter,
                                                    ServiceTokenAuthenticationFilter serviceTokenFilter)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public-at-HTTP-layer: anonymous + CUSTOMER can browse.
                        // The actual photographer-blocking lives on the controller methods
                        // via @PreAuthorize("!hasRole('PHOTOGRAPHER')"). The HTTP layer
                        // just needs to LET the request reach the controller first.
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/photographers",
                                "/api/v1/photographers/feed",
                                "/api/v1/photographers/{id}",
                                "/api/v1/photographers/{id}/portfolio",
                                "/api/v1/photographers/{id}/availability").permitAll()
                        // Observability + docs
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        // Everything else (all /me endpoints) requires authentication
                        .anyRequest().authenticated())
                // Service token first — if a Bearer JWT is present, it short-circuits
                // the gateway-headers path.
                .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(gatewayFilter, ServiceTokenAuthenticationFilter.class)
                .build();
    }
}
