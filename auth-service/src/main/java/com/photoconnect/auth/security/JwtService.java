package com.photoconnect.auth.security;

import com.photoconnect.auth.config.JwtProperties;
import com.photoconnect.auth.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * The only place in the codebase that touches the JWT private key.
 *
 * <p>Two distinct token types — access (short-lived, sent on every API call)
 * and refresh (longer-lived, only used to mint new access tokens). Both are
 * RS256-signed. The refresh token carries a {@code typ=refresh} claim so we
 * can reject access-tokens-as-refresh-tokens at parse time.</p>
 *
 * <p>The {@code jti} claim is a per-token random UUID and is what we record
 * in the {@code refresh_tokens} DB table and (for access tokens) what we
 * write to Redis when revoking on logout.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_ROLE  = "role";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_TYP   = "typ";
    public static final String TYP_ACCESS  = "access";
    public static final String TYP_REFRESH = "refresh";

    private final JwtProperties properties;

    private RSAPrivateKey privateKey;
    private RSAPublicKey  publicKey;

    @PostConstruct
    void loadKeys() throws Exception {
        // Load once at startup. Keys never change at runtime in MVP.
        // Phase 2: support hot rotation via /actuator/refresh + a key cache.
        Path privPath = Path.of(properties.privateKeyPath());
        Path pubPath  = Path.of(properties.publicKeyPath());
        log.info("Loading RSA keys: private={}, public={}", privPath, pubPath);
        this.privateKey = PemKeyLoader.loadPrivateKey(privPath);
        this.publicKey  = PemKeyLoader.loadPublicKey(pubPath);
    }

    /** Build a short-lived access token for the given user. */
    public IssuedToken generateAccessToken(UUID userId, String email, Role role) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant exp = now.plus(properties.accessTokenTtl());

        String token = Jwts.builder()
                .header().add("typ", "JWT").and()
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .subject(userId.toString())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TYP, TYP_ACCESS)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_EMAIL, email)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        return new IssuedToken(token, jti, exp);
    }

    /** Build a long-lived refresh token. The {@code jti} is what we store in DB. */
    public IssuedToken generateRefreshToken(UUID userId) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant exp = now.plus(properties.refreshTokenTtl());

        String token = Jwts.builder()
                .header().add("typ", "JWT").and()
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .subject(userId.toString())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TYP, TYP_REFRESH)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        return new IssuedToken(token, jti, exp);
    }

    /**
     * Verify signature + standard claims (iss, aud, exp). Returns the parsed
     * JWT or throws if anything is wrong. Callers are expected to ALSO check
     * the {@code typ} claim and any business invariants.
     */
    public Jws<Claims> parseAndVerify(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .build()
                .parseSignedClaims(token);
    }

    /** Bag-of-data return type for token mint operations. */
    public record IssuedToken(String token, String jti, Instant expiresAt) {}
}
