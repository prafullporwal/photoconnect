package com.photoconnect.photographer.security;

import com.photoconnect.photographer.config.ServiceJwtProperties;
import com.photoconnect.photographer.testutil.TestKeyPair;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link ServiceTokenAuthenticationFilter}.
 *
 * <p>Generates a real RSA key pair, signs JWTs with the private half, points
 * the filter at the public half, then asserts what authentication ends up in
 * the {@code SecurityContext} for each scenario.</p>
 */
class ServiceTokenAuthenticationFilterTest {

    @TempDir
    static Path keyDir;

    static RSAPrivateKey signingKey;
    static ServiceTokenAuthenticationFilter filter;

    static final String ISSUER = "photoconnect-test";
    static final String AUDIENCE = "photoconnect-api-test";

    @BeforeAll
    static void setup() throws Exception {
        // Generate a key pair; write public half to disk so PemKeyLoader can load it.
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        signingKey = (RSAPrivateKey) kp.getPrivate();

        Path pubFile = keyDir.resolve("public.pem");
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(kp.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        Files.writeString(pubFile, pem);

        RSAPublicKey publicKey = PemKeyLoader.loadPublicKey(pubFile);
        ServiceJwtProperties props = new ServiceJwtProperties(ISSUER, AUDIENCE, pubFile.toString());
        filter = new ServiceTokenAuthenticationFilter(props, publicKey);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validServiceTokenAuthenticatesWithRoleAndScope() throws Exception {
        String token = mintToken("customer-service", "service", "photographer-read", Instant.now().plusSeconds(60));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);

        FilterChain chain = new MockFilterChain();
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).extracting("authority")
                .contains("ROLE_SERVICE", "SCOPE_photographer-read");
        assertThat(auth.getPrincipal()).isInstanceOf(ServicePrincipal.class);
        assertThat(((ServicePrincipal) auth.getPrincipal()).clientId()).isEqualTo("customer-service");
    }

    @Test
    void tokenWithWrongTypIsIgnored() throws Exception {
        // typ=access — should NOT install a SERVICE authentication.
        String token = mintToken(UUID.randomUUID().toString(), "access", null, Instant.now().plusSeconds(60));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        String token = mintToken("customer-service", "service", "photographer-read", Instant.now().minusSeconds(10));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingHeaderLeavesContextEmpty() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /** Build a JWT with the given claims, signed by {@link #signingKey}. */
    private String mintToken(String sub, String typ, String scope, Instant exp) {
        var b = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(sub)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(exp))
                .claim("typ", typ);
        if (scope != null) {
            b = b.claim("scope", scope);
        }
        return b.signWith(signingKey, Jwts.SIG.RS256).compact();
    }
}
