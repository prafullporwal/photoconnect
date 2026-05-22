package com.photoconnect.reviews.config;

import com.photoconnect.reviews.security.GatewayAuthenticationFilter;
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
 * Stateless, header-based security.
 *
 * <p>Public reads: anyone (anonymous customers browsing the marketplace) can
 * see reviews + aggregate ratings for a photographer. Writes always require
 * a CUSTOMER token, enforced at the controller via {@code @PreAuthorize}.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    GatewayAuthenticationFilter gatewayFilter)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public reads: photographer detail page shows reviews to anonymous visitors.
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/reviews/photographer/**",
                                "/api/v1/reviews/summary/**").permitAll()
                        // Observability + docs
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(gatewayFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
