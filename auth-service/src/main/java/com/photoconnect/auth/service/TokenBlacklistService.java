package com.photoconnect.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis-backed blacklist for revoked access-token {@code jti}s.
 *
 * <p>Keys are written with a TTL equal to the access token's remaining life,
 * so Redis auto-evicts them when the token would have expired anyway.
 * Storage stays small no matter how many users log out.</p>
 *
 * <p>Trade-off note: a stateless JWT system has no built-in revocation. The
 * blacklist gives us opt-in revocation at the cost of one Redis lookup per
 * request that carries a token. That's a sub-millisecond ping in the same
 * VPC — acceptable.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    /** Blacklist the given access token jti until its natural expiry. */
    public void blacklist(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            // Already expired — no point storing it.
            return;
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
        } catch (Exception ex) {
            // Fail-open in Redis-unavailable scenarios: log and proceed. The
            // refresh token is already revoked in the DB, so the practical
            // damage window is just the access-token TTL (15 min).
            log.warn("Redis unavailable for blacklist write of jti={}: {}", jti, ex.getMessage());
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + jti));
        } catch (Exception ex) {
            // Fail-CLOSED would block all auth on a Redis outage. Fail-open
            // matches the trade-off above: brief revocation lag vs platform-
            // wide auth outage.
            log.warn("Redis unavailable for blacklist check of jti={}: {}", jti, ex.getMessage());
            return false;
        }
    }
}
