package com.photoconnect.auth.security;

import com.photoconnect.auth.config.JwtProperties;
import com.photoconnect.auth.domain.Role;
import com.photoconnect.auth.testutil.TestKeyPair;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@link JwtService}. Generates fresh RSA keys in a temp dir,
 * wires up a JwtService manually, and exercises generate/parse round trips
 * plus negative cases (expired, wrong issuer, tampered).
 */
class JwtServiceTest {

    @TempDir
    static Path tmp;

    static JwtService service;

    @BeforeAll
    static void setup() throws Exception {
        TestKeyPair.Paths paths = TestKeyPair.writeNew(tmp);
        JwtProperties props = new JwtProperties(
                "photoconnect-test",
                "photoconnect-api-test",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                paths.privateKey().toString(),
                paths.publicKey().toString());
        service = new JwtService(props);
        service.loadKeys();
    }

    @Test
    void accessTokenRoundTripsThroughVerification() {
        UUID userId = UUID.randomUUID();
        JwtService.IssuedToken issued = service.generateAccessToken(userId, "alice@example.com", Role.PHOTOGRAPHER);

        Jws<Claims> parsed = service.parseAndVerify(issued.token());
        Claims c = parsed.getPayload();

        assertThat(c.getSubject()).isEqualTo(userId.toString());
        assertThat(c.getId()).isEqualTo(issued.jti());
        assertThat(c.getIssuer()).isEqualTo("photoconnect-test");
        assertThat(c.get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("PHOTOGRAPHER");
        assertThat(c.get(JwtService.CLAIM_EMAIL, String.class)).isEqualTo("alice@example.com");
        assertThat(c.get(JwtService.CLAIM_TYP, String.class)).isEqualTo(JwtService.TYP_ACCESS);
    }

    @Test
    void refreshTokenCarriesRefreshTyp() {
        JwtService.IssuedToken issued = service.generateRefreshToken(UUID.randomUUID());
        Claims c = service.parseAndVerify(issued.token()).getPayload();
        assertThat(c.get(JwtService.CLAIM_TYP, String.class)).isEqualTo(JwtService.TYP_REFRESH);
    }

    @Test
    void tamperedSignatureIsRejected() {
        JwtService.IssuedToken issued = service.generateAccessToken(UUID.randomUUID(), "x@y.z", Role.CUSTOMER);
        // Flip a character in the signature segment
        String[] parts = issued.token().split("\\.");
        char[] sig = parts[2].toCharArray();
        sig[0] = sig[0] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + new String(sig);

        assertThatThrownBy(() -> service.parseAndVerify(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void emptyTokenIsRejected() {
        assertThatThrownBy(() -> service.parseAndVerify(""))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }
}
