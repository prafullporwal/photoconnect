package com.photoconnect.customer.security;

import com.photoconnect.customer.domain.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads the trusted identity headers stamped by api-gateway and establishes
 * the Spring Security {@code Authentication}. Same pattern as photographer-service.
 */
@Slf4j
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID   = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String roleHeader   = request.getHeader(HEADER_USER_ROLE);

        if (userIdHeader != null && !userIdHeader.isBlank()
                && roleHeader != null && !roleHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader);
                Role role   = Role.valueOf(roleHeader);

                GatewayPrincipal principal = new GatewayPrincipal(userId, role);
                var auth = UsernamePasswordAuthenticationToken.authenticated(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                log.warn("Malformed gateway identity headers — X-User-Id={}, X-User-Role={}",
                        userIdHeader, roleHeader);
            }
        }

        chain.doFilter(request, response);
    }
}
