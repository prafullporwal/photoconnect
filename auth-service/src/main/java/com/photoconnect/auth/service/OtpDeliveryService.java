package com.photoconnect.auth.service;

/**
 * Abstraction over how an OTP reaches the user. Implementations:
 *
 * <ul>
 *   <li>{@link DevModeOtpDeliveryService} — logs the code to stdout.
 *       Bound when {@code app.otp.dev-mode=true}.</li>
 *   <li>(Stage 2) {@code Msg91OtpDeliveryService}, {@code TwilioOtpDeliveryService}, …</li>
 * </ul>
 *
 * <p>Keeping this as an interface means the rest of the OTP code never sees
 * a vendor SDK type — the Stage 2 swap is a single {@code @Bean} change.</p>
 */
public interface OtpDeliveryService {

    /**
     * Deliver {@code code} to {@code phone}. Implementations should throw on
     * permanent failure; transient failures are retried by the caller.
     */
    void deliver(String phone, String code);
}
