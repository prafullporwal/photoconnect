package com.photoconnect.customer.config;

import com.photoconnect.customer.security.GatewayAuthenticationFilter;
import com.photoconnect.customer.security.ServiceTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless, header-based security with a second path for service-to-service
 * traffic. Mirrors photographer-service:
 *
 * <ol>
 *   <li><b>User requests via the gateway.</b> Identity headers
 *       ({@code X-User-Id}, {@code X-User-Role}) are read by
 *       {@link GatewayAuthenticationFilter}.</li>
 *   <li><b>Service-to-service requests via Eureka.</b> A Bearer service-JWT is
 *       verified by {@link ServiceTokenAuthenticationFilter}, installing a
 *       {@code ROLE_SERVICE} + {@code SCOPE_*} authentication.</li>
 * </ol>
 *
 * <p>The service-token filter runs FIRST. If it populates the context the
 * gateway-headers filter sees a non-null Authentication and bails. If no Bearer
 * header is present the service-token filter is a no-op and the gateway-headers
 * filter handles the user case.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

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
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(gatewayFilter, ServiceTokenAuthenticationFilter.class)
                .build();
    }
}
