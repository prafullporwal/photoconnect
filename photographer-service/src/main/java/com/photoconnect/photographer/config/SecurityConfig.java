package com.photoconnect.photographer.config;

import com.photoconnect.photographer.security.GatewayAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for photographer-service.
 *
 * <ul>
 *   <li>Stateless — no HTTP sessions. Identity is re-established per request
 *       from the gateway headers via {@link GatewayAuthenticationFilter}.</li>
 *   <li>CSRF off — stateless API, no cookie-based auth.</li>
 *   <li>CORS off — handled at the gateway.</li>
 *   <li>Public: {@code GET /api/v1/photographers} and
 *       {@code GET /api/v1/photographers/{id}} (browsing the marketplace).</li>
 *   <li>Everything else requires authentication (i.e. the gateway headers
 *       must be present and valid).</li>
 *   <li>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} on
 *       controller and service methods.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * {@link GatewayAuthenticationFilter} is a method parameter (not a
     * constructor field) so that {@code @WebMvcTest} slices can provide a
     * no-op stub without breaking the whole configuration class.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    GatewayAuthenticationFilter gatewayFilter)
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
                                "/api/v1/photographers/{id}/portfolio").permitAll()
                        // Observability + docs
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        // Everything else (all /me endpoints) requires authentication
                        .anyRequest().authenticated())
                // Register our header-based filter in the Security chain so that
                // SecurityContextHolderFilter manages the context lifecycle correctly.
                .addFilterBefore(gatewayFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
