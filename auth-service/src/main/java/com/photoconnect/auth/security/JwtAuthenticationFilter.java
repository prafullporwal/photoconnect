package com.photoconnect.auth.security;

import com.photoconnect.auth.domain.Role;
import com.photoconnect.auth.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads a Bearer JWT off the {@code Authorization} header (when present),
 * verifies it, checks the Redis blacklist, and attaches a
 * {@link UserPrincipal}-backed {@code Authentication} to the
 * {@code SecurityContext}.
 *
 * <p>If the header is missing or the token is invalid we do NOT throw —
 * we just leave the SecurityContext empty and let downstream authorization
 * decide. That keeps {@code permitAll()} endpoints reachable without a token.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Jws<Claims> jws = jwtService.parseAndVerify(token);
            Claims claims = jws.getPayload();

            // Reject refresh tokens used as access tokens.
            String typ = claims.get(JwtService.CLAIM_TYP, String.class);
            if (!JwtService.TYP_ACCESS.equals(typ)) {
                log.debug("Rejecting JWT with typ={} on protected endpoint", typ);
                filterChain.doFilter(request, response);
                return;
            }

            String jti = claims.getId();
            if (blacklist.isBlacklisted(jti)) {
                log.debug("Rejecting blacklisted access token jti={}", jti);
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(JwtService.CLAIM_EMAIL, String.class);
            Role role = Role.valueOf(claims.get(JwtService.CLAIM_ROLE, String.class));

            UserPrincipal principal = new UserPrincipal(userId, email, role, jti);
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            // Leave SecurityContext empty; permitAll() endpoints still work,
            // protected endpoints will return 401 via Spring Security's
            // entry point.
        }

        filterChain.doFilter(request, response);
    }
}
