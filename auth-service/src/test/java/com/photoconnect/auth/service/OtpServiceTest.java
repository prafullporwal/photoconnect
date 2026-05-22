package com.photoconnect.auth.service;

import com.photoconnect.auth.config.OtpProperties;
import com.photoconnect.auth.exception.InvalidOtpException;
import com.photoconnect.auth.exception.OtpCooldownException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link OtpService}. Mocks {@link StringRedisTemplate} so we
 * exercise the OTP state machine (cooldown, attempts, single-use) without
 * standing up a real Redis. A small in-memory {@link Map} backs the value-ops
 * stub so we can assert on what was written.
 */
class OtpServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private CapturingDelivery delivery;
    private OtpService otp;
    private final Map<String, String> store = new HashMap<>();
    private final Map<String, Boolean> cooldownTaken = new HashMap<>();

    private static final OtpProperties PROPS = new OtpProperties(
            OtpProperties.Provider.DEV, true, 6, Duration.ofMinutes(5), Duration.ofSeconds(30), 5);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        store.clear();
        cooldownTaken.clear();

        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        // setIfAbsent: simulates Redis SET NX EX semantics for the cooldown key.
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(0);
                    if (cooldownTaken.containsKey(key)) return false;
                    cooldownTaken.put(key, true);
                    return true;
                });
        when(redis.getExpire(anyString())).thenReturn(30L);

        // plain set + get against our in-memory map
        when(valueOps.get(anyString())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        org.mockito.Mockito.doAnswer(inv -> {
            store.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        // decrement: parse-int, subtract, store back, return as Long
        when(valueOps.decrement(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            int current = Integer.parseInt(store.getOrDefault(key, "0"));
            int next = current - 1;
            store.put(key, Integer.toString(next));
            return (long) next;
        });
        when(redis.delete(anyString())).thenAnswer(inv -> store.remove(inv.getArgument(0)) != null);

        delivery = new CapturingDelivery();
        otp = new OtpService(redis, PROPS, delivery);
    }

    @Test
    void send_storesCodeAndAttempts_andDelivers() {
        OtpService.Issued issued = otp.sendOtp("+919876543210");

        assertThat(issued.code()).hasSize(6).matches("\\d{6}");
        assertThat(issued.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(delivery.lastPhone).isEqualTo("+919876543210");
        assertThat(delivery.lastCode).isEqualTo(issued.code());

        // Redis writes: code, attempts, and cooldown (via setIfAbsent)
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps, atLeastOnce()).set(keyCaptor.capture(), anyString(), any(Duration.class));
        assertThat(keyCaptor.getAllValues())
                .anyMatch(k -> k.equals("auth:otp:code:+919876543210"))
                .anyMatch(k -> k.equals("auth:otp:attempts:+919876543210"));
    }

    @Test
    void send_twice_inCooldown_throws() {
        otp.sendOtp("+919876543210");
        assertThatThrownBy(() -> otp.sendOtp("+919876543210"))
                .isInstanceOf(OtpCooldownException.class);
    }

    @Test
    void verify_correctCode_succeeds_andWipesKeys() {
        String code = otp.sendOtp("+919876543210").code();

        boolean ok = otp.verify("+919876543210", code);

        assertThat(ok).isTrue();
        // both keys deleted on success
        verify(redis).delete("auth:otp:code:+919876543210");
        verify(redis).delete("auth:otp:attempts:+919876543210");
    }

    @Test
    void verify_wrongCode_decrementsAndThrows() {
        otp.sendOtp("+919876543210");

        assertThatThrownBy(() -> otp.verify("+919876543210", "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("attempt(s) left");

        verify(valueOps).decrement("auth:otp:attempts:+919876543210");
    }

    @Test
    void verify_noActiveCode_throws() {
        assertThatThrownBy(() -> otp.verify("+919876543210", "123456"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("no active code");
    }

    @Test
    void send_deliveryFails_wipesAllKeysAndRethrows() {
        // Replace the well-behaved delivery with one that always throws — the
        // service must clean up so the user can retry immediately.
        OtpDeliveryService failing = (phone, code) -> {
            throw new RuntimeException("provider down");
        };
        OtpService svc = new OtpService(redis, PROPS, failing);

        assertThatThrownBy(() -> svc.sendOtp("+919876543210"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("provider down");

        verify(redis).delete("auth:otp:code:+919876543210");
        verify(redis).delete("auth:otp:attempts:+919876543210");
        verify(redis).delete("auth:otp:cooldown:+919876543210");
    }

    @Test
    void verify_exhaustsAttempts_wipesCode() {
        String code = otp.sendOtp("+919876543210").code();
        String wrong = code.equals("000000") ? "111111" : "000000";

        // 5 wrong attempts max — fifth should wipe
        for (int i = 0; i < 5; i++) {
            try { otp.verify("+919876543210", wrong); } catch (InvalidOtpException ignored) {}
        }

        // Code key gone — sixth attempt is "no active code"
        assertThatThrownBy(() -> otp.verify("+919876543210", code))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("no active code");
        verify(redis, times(1)).delete(eq("auth:otp:code:+919876543210"));
    }

    /** Captures the last delivered (phone, code) so tests can assert on it. */
    private static final class CapturingDelivery implements OtpDeliveryService {
        String lastPhone;
        String lastCode;
        @Override public void deliver(String phone, String code) {
            this.lastPhone = phone;
            this.lastCode  = code;
        }
    }
}
