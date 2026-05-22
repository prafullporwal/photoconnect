package com.photoconnect.customer.security;

import com.photoconnect.customer.config.ServiceJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Verifies service-to-service JWTs on incoming requests. Same shape as the
 * photographer-service variant.
 *
 * <p>If a Bearer token is present and verifies as {@code typ=service}, this
 * filter installs a {@code ROLE_SERVICE} + {@code SCOPE_*} authentication and
 * {@link GatewayAuthenticationFilter} sees a populated context and bails. If
 * no Bearer header is present, this is a no-op and the gateway-headers filter
 * handles things normally.</p>
 */
@Slf4j
@Component
public class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String CLAIM_TYP   = "typ";
    public static final String CLAIM_SCOPE = "scope";
    public static final String TYP_SERVICE = "service";
    public static final String ROLE_SERVICE = "ROLE_SERVICE";
    public static final String AUTHORITY_SCOPE_PREFIX = "SCOPE_";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ServiceJwtProperties properties;
    private final RSAPublicKey publicKey;

    public ServiceTokenAuthenticationFilter(ServiceJwtProperties properties, RSAPublicKey publicKey) {
        this.properties = properties;
        this.publicKey = publicKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(properties.issuer())
                    .requireAudience(properties.audience())
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            String typ = claims.get(CLAIM_TYP, String.class);
            if (!TYP_SERVICE.equals(typ)) {
                log.trace("Bearer token typ={} is not 'service'; deferring to gateway-headers filter", typ);
                chain.doFilter(request, response);
                return;
            }

            String clientId = claims.getSubject();
            String scope = claims.get(CLAIM_SCOPE, String.class);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(ROLE_SERVICE));
            if (scope != null && !scope.isBlank()) {
                Arrays.stream(scope.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(s -> authorities.add(new SimpleGrantedAuthority(AUTHORITY_SCOPE_PREFIX + s)));
            }

            ServicePrincipal principal = new ServicePrincipal(clientId, scope);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authenticated service caller clientId={} scope={}", clientId, scope);

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Service JWT validation failed: {}", ex.getMessage());
            // Leave SecurityContext empty; Spring Security will 401 on protected endpoints.
        }

        chain.doFilter(request, response);
    }
}
