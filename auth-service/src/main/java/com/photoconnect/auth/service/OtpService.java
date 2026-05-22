package com.photoconnect.auth.service;

import com.photoconnect.auth.config.OtpProperties;
import com.photoconnect.auth.exception.InvalidOtpException;
import com.photoconnect.auth.exception.OtpCooldownException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * OTP minting, storage and verification.
 *
 * <p>Two Redis keys per phone:</p>
 * <ul>
 *   <li>{@code auth:otp:code:<phone>} — the code itself, TTL = {@code app.otp.ttl}</li>
 *   <li>{@code auth:otp:attempts:<phone>} — remaining verify attempts, same TTL</li>
 *   <li>{@code auth:otp:cooldown:<phone>} — set on send, TTL = {@code app.otp.resendCooldown}</li>
 * </ul>
 *
 * <p>On successful verify both code+attempts keys are deleted (single-use).
 * Cooldown is intentionally not cleared — a successful login does NOT entitle
 * the user to immediately spam new codes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String K_CODE     = "auth:otp:code:";
    private static final String K_ATTEMPTS = "auth:otp:attempts:";
    private static final String K_COOLDOWN = "auth:otp:cooldown:";

    private final StringRedisTemplate redis;
    private final OtpProperties props;
    private final OtpDeliveryService delivery;
    private final SecureRandom random = new SecureRandom();

    /** Returns the generated code so dev-mode can echo it; callers MUST guard exposure. */
    public Issued sendOtp(String phone) {
        // 1) cooldown check — gives setIfAbsent atomicity so two near-simultaneous
        //    sends from the same phone can't both succeed.
        Boolean acquired = redis.opsForValue().setIfAbsent(
                K_COOLDOWN + phone, "1", props.resendCooldown());
        if (Boolean.FALSE.equals(acquired)) {
            Long remaining = redis.getExpire(K_COOLDOWN + phone);
            throw new OtpCooldownException(remaining == null ? 0 : remaining);
        }

        // 2) mint, store, deliver
        String code = generateCode();
        Duration ttl = props.ttl();
        redis.opsForValue().set(K_CODE + phone, code, ttl);
        redis.opsForValue().set(K_ATTEMPTS + phone, Integer.toString(props.maxAttempts()), ttl);

        try {
            delivery.deliver(phone, code);
        } catch (RuntimeException ex) {
            // Delivery failed — the user never got this code. Clean up so we
            // don't leave a stale, unusable code in Redis, and drop the cooldown
            // so the user can immediately retry (they didn't burn their one shot).
            // The cooldown is anti-abuse for successful deliveries, not a tax
            // on the user when our provider is having a bad day.
            redis.delete(K_CODE + phone);
            redis.delete(K_ATTEMPTS + phone);
            redis.delete(K_COOLDOWN + phone);
            throw ex;
        }

        return new Issued(code, Instant.now().plus(ttl));
    }

    /**
     * Returns true on success. On wrong code we decrement attempts and throw;
     * on no-code-found or no-attempts-left we also throw. Caller can rely on
     * a true return to mean "this phone is verified RIGHT NOW".
     */
    public boolean verify(String phone, String submitted) {
        String stored = redis.opsForValue().get(K_CODE + phone);
        if (stored == null) {
            throw new InvalidOtpException("no active code for this phone");
        }

        if (!constantTimeEquals(stored, submitted)) {
            // Decrement attempts; if we hit zero, wipe the code so it can't be
            // brute-forced further before its natural TTL.
            Long left = redis.opsForValue().decrement(K_ATTEMPTS + phone);
            if (left == null || left <= 0) {
                redis.delete(K_CODE + phone);
                redis.delete(K_ATTEMPTS + phone);
                throw new InvalidOtpException("too many wrong attempts");
            }
            throw new InvalidOtpException("incorrect code; " + left + " attempt(s) left");
        }

        // Single-use: wipe both keys on success.
        redis.delete(K_CODE + phone);
        redis.delete(K_ATTEMPTS + phone);
        log.info("OTP verified for phone={}", phone);
        return true;
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, props.length());
        int n = random.nextInt(bound);
        return String.format("%0" + props.length() + "d", n);
    }

    /** Avoid early-exit comparisons leaking timing info about prefix matches. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    /** Carries the minted code (for dev-mode echo) and its absolute expiry. */
    public record Issued(String code, Instant expiresAt) {}
}
