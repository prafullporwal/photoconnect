package com.photoconnect.auth.service;

import com.photoconnect.auth.config.ServiceClientsProperties;
import com.photoconnect.auth.dto.ServiceTokenRequest;
import com.photoconnect.auth.dto.ServiceTokenResponse;
import com.photoconnect.auth.exception.InvalidServiceClientException;
import com.photoconnect.auth.security.JwtService;
import com.photoconnect.auth.security.TestJwtServiceFactory;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@link ServiceTokenService}.
 *
 * <p>Wires a real {@link JwtService} (so we exercise the round trip through
 * RS256 signing) and a real {@link BCryptPasswordEncoder} (so we exercise the
 * real secret-comparison path), then drives the service directly. No Spring
 * context.</p>
 */
class ServiceTokenServiceTest {

    @TempDir
    static Path tmp;

    static JwtService jwtService;
    static PasswordEncoder encoder;
    static ServiceTokenService tokenService;
    static String validHash;

    @BeforeAll
    static void setup() throws Exception {
        jwtService = TestJwtServiceFactory.createWithFreshKeys(tmp);

        encoder = new BCryptPasswordEncoder(12);
        validHash = encoder.encode("super-secret-shh");

        ServiceClientsProperties props = new ServiceClientsProperties(
                Duration.ofSeconds(60),
                Map.of("customer-service",
                        new ServiceClientsProperties.Client(validHash, "photographer-read")));
        tokenService = new ServiceTokenService(props, encoder, jwtService);
    }

    @Test
    void mintsTokenWhenCredentialsMatch() {
        ServiceTokenResponse resp = tokenService.issue(
                new ServiceTokenRequest("customer-service", "super-secret-shh"));

        assertThat(resp.accessToken()).isNotBlank();
        assertThat(resp.scope()).isEqualTo("photographer-read");
        assertThat(resp.expiresAt()).isAfter(java.time.Instant.now());

        // Round-trip the JWT through the same verifier auth-service uses for
        // user tokens — it must verify and carry typ=service, sub=clientId, scope.
        Claims claims = jwtService.parseAndVerify(resp.accessToken()).getPayload();
        assertThat(claims.getSubject()).isEqualTo("customer-service");
        assertThat(claims.get(JwtService.CLAIM_TYP, String.class)).isEqualTo(JwtService.TYP_SERVICE);
        assertThat(claims.get(JwtService.CLAIM_SCOPE, String.class)).isEqualTo("photographer-read");
    }

    @Test
    void rejectsUnknownClient() {
        assertThatThrownBy(() -> tokenService.issue(
                new ServiceTokenRequest("ghost-service", "whatever")))
                .isInstanceOf(InvalidServiceClientException.class);
    }

    @Test
    void rejectsWrongSecret() {
        assertThatThrownBy(() -> tokenService.issue(
                new ServiceTokenRequest("customer-service", "wrong-secret")))
                .isInstanceOf(InvalidServiceClientException.class);
    }
}
