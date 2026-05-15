package com.photoconnect.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photoconnect.gateway.config.JwtGatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;

/**
 * Global filter that enforces JWT authentication on every request entering the
 * platform, with explicit carve-outs for public endpoints.
 *
 * <h2>Why validate at the gateway?</h2>
 * <p>Downstream services also validate JWTs (defence-in-depth), but the
 * gateway is the right place to:</p>
 * <ul>
 *   <li>Reject bad tokens before any downstream I/O is triggered.</li>
 *   <li>Extract the identity once and forward it as trusted headers
 *       ({@code X-User-Id}, {@code X-User-Role}) so downstream code reads a
 *       header rather than re-parsing the JWT on every request.</li>
 *   <li>Return a single, consistent 401 JSON shape for the front-end.</li>
 * </ul>
 *
 * <h2>What is validated</h2>
 * <ol>
 *   <li>Token signature — verified with the RSA-2048 public key.</li>
 *   <li>Expiry ({@code exp}) — jjwt rejects expired tokens automatically.</li>
 *   <li>Issuer ({@code iss}) — must equal {@code gateway.jwt.issuer}.</li>
 *   <li>Audience ({@code aud}) — must contain {@code gateway.jwt.audience}.</li>
 *   <li>Token type ({@code typ}) — must be {@code "access"}; refresh tokens
 *       are valid only for the {@code /api/v1/auth/refresh} endpoint.</li>
 * </ol>
 *
 * <h2>Header forwarding</h2>
 * <p>After a successful validation the filter mutates the downstream request to
 * add {@value #HEADER_USER_ID} (the UUID subject) and
 * {@value #HEADER_USER_ROLE} (the role claim). These headers replace the JWT
 * inside the platform: downstream services should trust them and never expose
 * them to external callers.</p>
 *
 * <h2>Order relative to {@link CorrelationIdFilter}</h2>
 * <p>{@link CorrelationIdFilter} runs at {@link Ordered#HIGHEST_PRECEDENCE} so
 * all subsequent log lines — including our own 401 responses — carry a
 * correlation ID. This filter runs one priority later at
 * {@code HIGHEST_PRECEDENCE + 1}.</p>
 */
@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    /** Custom JWT claim carrying the token type ({@code "access"} or {@code "refresh"}). */
    private static final String CLAIM_TYP = "typ";

    /** Custom JWT claim carrying the user's role. */
    private static final String CLAIM_ROLE = "role";

    /** Forwarded to downstream services: the authenticated user's UUID. */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** Forwarded to downstream services: the authenticated user's role name. */
    public static final String HEADER_USER_ROLE = "X-User-Role";

    private final JwtGatewayProperties properties;
    private final RSAPublicKey publicKey;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper;

    /**
     * Spring-managed constructor — the RSA public key is loaded once at startup by
     * {@link com.photoconnect.gateway.config.GatewaySecurityConfig} and injected here.
     */
    public JwtValidationFilter(JwtGatewayProperties properties,
                                RSAPublicKey publicKey,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        this.publicKey = publicKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getRawPath();
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean hasBearer = authHeader != null && authHeader.startsWith(BEARER_PREFIX);
        boolean publicPath = isPublicPath(path);

        // ── No token: allowed only on public paths ────────────────────────────
        // A "public path" means *anonymous access is permitted*, not that tokens
        // get skipped. If a token IS present we still validate it below — that's
        // how marketplace browse (anonymous) and /me (authenticated) can share
        // a URL prefix without making the gateway leak identity headers.
        if (!hasBearer) {
            if (publicPath) {
                return chain.filter(exchange);
            }
            log.debug("Rejected request to {}: missing or malformed Authorization header", path);
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).strip();

        // ── Verify signature, expiry, issuer, audience ────────────────────────
        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(properties.issuer())
                    .requireAudience(properties.audience())
                    .build()
                    .parseSignedClaims(token);
        } catch (JwtException e) {
            log.debug("JWT validation failed for path {}: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }

        Claims claims = jws.getPayload();

        // ── Reject refresh tokens on API endpoints ────────────────────────────
        // Refresh tokens are credentials for /auth/refresh only, not for protected APIs.
        String tokenType = claims.get(CLAIM_TYP, String.class);
        if (!"access".equals(tokenType)) {
            log.debug("Rejected request to {}: token type '{}' is not 'access'", path, tokenType);
            return unauthorized(exchange, "Token type not permitted for API access");
        }

        // ── Forward identity to downstream ────────────────────────────────────
        // Downstream services read X-User-Id / X-User-Role instead of re-parsing the JWT.
        // IMPORTANT: strip any client-supplied versions of these headers first to prevent spoofing.
        String userId = claims.getSubject();
        String role   = claims.get(CLAIM_ROLE, String.class);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> h.remove(HEADER_USER_ID))   // strip client-supplied (spoofing defence)
                .headers(h -> h.remove(HEADER_USER_ROLE))
                .header(HEADER_USER_ID,   userId)
                .header(HEADER_USER_ROLE, role != null ? role : "")
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return properties.publicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Write a 401 JSON response directly to the reactive response and complete
     * the publisher chain. No controller is involved — this is a WebFlux filter.
     *
     * <p>The shape mirrors {@code ErrorResponse} in auth-service so the front-end
     * has a consistent error envelope across the whole platform.</p>
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "status",    401,
                "message",   message,
                "timestamp", Instant.now().toString()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            // Fallback: shouldn't happen with the simple types above
            bytes = "{\"status\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run immediately after CorrelationIdFilter (HIGHEST_PRECEDENCE)
        // so rejected requests still get a correlation ID in their 401 response.
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
