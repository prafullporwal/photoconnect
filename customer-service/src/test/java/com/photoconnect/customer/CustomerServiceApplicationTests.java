package com.photoconnect.customer;

import com.photoconnect.customer.testutil.TestKeyPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

/**
 * Smoke test: the Spring context must load without errors.
 *
 * <p>Runs against the {@code test} profile so the app does NOT try to reach
 * MySQL, Eureka, or the Config Server at startup. A throw-away RSA public key
 * satisfies the {@code customer.service-jwt.public-key-path} property added
 * for inbound service-token validation.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class CustomerServiceApplicationTests {

    @TempDir
    static Path keyDir;

    @DynamicPropertySource
    static void serviceJwtKey(DynamicPropertyRegistry registry) throws Exception {
        Path pub = TestKeyPair.writePublicKey(keyDir);
        registry.add("customer.service-jwt.public-key-path", pub::toString);
    }

    @Test
    void contextLoads() {
        // If the context fails to start, this test fails with a helpful
        // BeanCreationException rather than a silent "0 tests found" result.
    }
}
