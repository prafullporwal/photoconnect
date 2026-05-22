package com.photoconnect.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Dev/local delivery: prints the OTP to the service log so the developer can
 * paste it into the next request.
 *
 * <p>Active when {@code app.otp.provider=dev} (or unset — {@code matchIfMissing}
 * keeps dev as the safe default if the property is missing entirely). The
 * MSG91 impl has the complementary {@code havingValue="msg91"} condition, so
 * exactly one delivery bean is created.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.otp.provider", havingValue = "dev", matchIfMissing = true)
public class DevModeOtpDeliveryService implements OtpDeliveryService {

    @Override
    public void deliver(String phone, String code) {
        log.info("[DEV-OTP] phone={} code={} — DO NOT ENABLE IN PROD", phone, code);
    }
}
