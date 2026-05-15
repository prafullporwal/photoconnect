package com.photoconnect.photographer.security;

import com.photoconnect.photographer.domain.Role;
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
 * Establishes the Spring Security {@code Authentication} from the trusted
 * identity headers stamped by api-gateway.
 *
 * <h2>Why "trusted"?</h2>
 * <p>api-gateway has already verified the JWT signature, expiry, issuer, and
 * audience. It then <em>strips</em> any client-supplied {@code X-User-Id} /
 * {@code X-User-Role} headers and replaces them with values extracted from the
 * verified token. By the time a request reaches this service, those headers
 * can be trusted as authoritative.</p>
 *
 * <p>(In production, the internal network must prevent direct calls to
 * photographer-service that bypass the gateway — security groups / service
 * mesh policy enforce this. In dev, be aware that direct calls skip JWT
 * validation.)</p>
 *
 * <h2>What this filter does</h2>
 * <ol>
 *   <li>Reads {@code X-User-Id} and {@code X-User-Role} from the request.</li>
 *   <li>Parses them into a {@link GatewayPrincipal}.</li>
 *   <li>Creates a fully-authenticated {@link UsernamePasswordAuthenticationToken}
 *       with the principal and a {@code ROLE_*} granted authority.</li>
 *   <li>Stores it in {@code SecurityContextHolder} so Spring Security's
 *       authorization rules and {@code @AuthenticationPrincipal} injection
 *       work downstream.</li>
 *   <li>If the headers are absent or malformed, the {@code SecurityContext}
 *       stays empty — the request is treated as anonymous, and Spring Security
 *       enforces the configured access rules.</li>
 * </ol>
 *
 * <p>This filter is registered inside the Spring Security filter chain (via
 * {@link com.photoconnect.photographer.config.SecurityConfig}) so the
 * {@code SecurityContextHolderFilter} manages context lifecycle. We must NOT
 * call {@code SecurityContextHolder.clearContext()} here.</p>
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
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (IllegalArgumentException e) {
                // Malformed UUID or unknown role — leave the context empty so
                // Spring Security treats this as anonymous and enforces rules normally.
                log.warn("Malformed gateway identity headers — X-User-Id={}, X-User-Role={}",
                        userIdHeader, roleHeader);
            }
        }

        chain.doFilter(request, response);
    }
}
