package com.photoconnect.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.photoconnect.gateway.config.JwtGatewayProperties;
import com.photoconnect.gateway.testutil.TestPublicKey;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtValidationFilter}.
 *
 * <p>Pure unit test: no Spring context, no file I/O. The filter is
 * constructed directly with an in-memory RSA key pair from
 * {@link TestPublicKey}.</p>
 *
 * <p>Pattern notes:</p>
 * <ul>
 *   <li>The chain lambda captures the exchange passed to it so we can assert
 *       on the mutated request headers forwarded downstream.</li>
 *   <li>{@code .block()} drives the reactive chain synchronously — fine for
 *       unit tests where there is no real I/O.</li>
 *   <li>We check the 401 shape by reading the status code on the
 *       {@link MockServerWebExchange} response — the filter writes directly
 *       to the response and never calls {@code chain.filter(...)}.</li>
 * </ul>
 */
class JwtValidationFilterTest {

    // ── The filter under test ─────────────────────────────────────────────────

    private JwtValidationFilter filter;

    @BeforeEach
    void setUp() {
        JwtGatewayProperties props = new JwtGatewayProperties(
                "irrelevant-for-unit-test",   // publicKeyPath - only used by GatewaySecurityConfig
                "photoconnect",               // issuer
                "photoconnect-api",           // audience
                List.of(
                        "/api/v1/auth/**",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                )
        );
        filter = new JwtValidationFilter(props, TestPublicKey.publicKey(), new ObjectMapper());
    }

    // ── Public path bypass ────────────────────────────────────────────────────

    @Test
    void publicPath_skips_jwtValidation_andCallsChain() {
        MockServerWebExchange exchange = exchange("GET", "/api/v1/auth/login");
        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);

        filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled.get())
                .as("chain must be called for public paths (no auth required)")
                .isTrue();
        // No status change means the filter left the response alone
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void publicPath_actuator_skips_jwtValidation() {
        MockServerWebExchange exchange = exchange("GET", "/actuator/health");
        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);

        filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled.get()).isTrue();
    }

    // ── Missing / malformed Authorization header ──────────────────────────────

    @Test
    void missingAuthHeader_returns401() {
        MockServerWebExchange exchange = exchange("GET", "/api/v1/photographers/me");

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode())
                .as("must return 401 when Authorization header is absent")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void nonBearerAuthHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/photographers/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Valid token ───────────────────────────────────────────────────────────

    @Test
    void validAccessToken_callsChain_withUserIdAndRoleHeaders() {
        String userId = UUID.randomUUID().toString();
        String token = buildAccessToken(userId, "alice@example.com", "PHOTOGRAPHER");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/photographers/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            downstream.set(ex);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode())
                .as("no 401 should be set on valid token")
                .isNull();
        assertThat(downstream.get()).as("chain must have been called").isNotNull();

        HttpHeaders downstreamHeaders = downstream.get().getRequest().getHeaders();
        assertThat(downstreamHeaders.getFirst(JwtValidationFilter.HEADER_USER_ID))
                .as("X-User-Id must carry the token subject (UUID)")
                .isEqualTo(userId);
        assertThat(downstreamHeaders.getFirst(JwtValidationFilter.HEADER_USER_ROLE))
                .as("X-User-Role must carry the role claim")
                .isEqualTo("PHOTOGRAPHER");
    }

    @Test
    void spoofedUserIdHeader_isStripped_beforeForwarding() {
        // A client that tries to impersonate by sending X-User-Id directly
        String realUserId = UUID.randomUUID().toString();
        String token = buildAccessToken(realUserId, "alice@example.com", "PHOTOGRAPHER");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/photographers/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(JwtValidationFilter.HEADER_USER_ID,   "spoofed-id")
                .header(JwtValidationFilter.HEADER_USER_ROLE, "ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            downstream.set(ex);
            return Mono.empty();
        }).block();

        HttpHeaders downstreamHeaders = downstream.get().getRequest().getHeaders();
        assertThat(downstreamHeaders.getFirst(JwtValidationFilter.HEADER_USER_ID))
                .as("client-supplied X-User-Id must be replaced with the JWT subject")
                .isEqualTo(realUserId);
        assertThat(downstreamHeaders.getFirst(JwtValidationFilter.HEADER_USER_ROLE))
                .as("client-supplied X-User-Role must be replaced with the JWT claim")
                .isEqualTo("PHOTOGRAPHER");
    }

    // ── Invalid / expired tokens ──────────────────────────────────────────────

    @Test
    void expiredToken_returns401() {
        String token = buildExpiredToken(UUID.randomUUID().toString());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/photographers/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void randomGarbage_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/photographers/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.jwt")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshToken_returns401() {
        // Refresh tokens must not work as API credentials
        String token = buildRefreshToken(UUID.randomUUID().toString());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/photographers/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static MockServerWebExchange exchange(String method, String path) {
        MockServerHttpRequest request = MockServerHttpRequest.method(
                org.springframework.http.HttpMethod.valueOf(method), path).build();
        return MockServerWebExchange.from(request);
    }

    private static String buildAccessToken(String subject, String email, String role) {
        return Jwts.builder()
                .subject(subject)
                .issuer("photoconnect")
                .audience().add("photoconnect-api").and()
                .claim("typ",   "access")
                .claim("role",  role)
                .claim("email", email)
                .claim("jti",   UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(TestPublicKey.privateKey(), Jwts.SIG.RS256)
                .compact();
    }

    private static String buildExpiredToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuer("photoconnect")
                .audience().add("photoconnect-api").and()
                .claim("typ", "access")
                .expiration(Date.from(Instant.now().minusSeconds(60)))  // already expired
                .signWith(TestPublicKey.privateKey(), Jwts.SIG.RS256)
                .compact();
    }

    private static String buildRefreshToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuer("photoconnect")
                .audience().add("photoconnect-api").and()
                .claim("typ", "refresh")   // wrong type
                .expiration(Date.from(Instant.now().plusSeconds(604800)))
                .signWith(TestPublicKey.privateKey(), Jwts.SIG.RS256)
                .compact();
    }
}
