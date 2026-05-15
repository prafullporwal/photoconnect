package com.photoconnect.gateway;

import com.photoconnect.gateway.testutil.TestPublicKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Smoke test for the gateway's Spring context.
 *
 * <p>We disable Config Server and Eureka so the context can boot standalone —
 * this test verifies that the gateway's own beans (routes, filters, JWT config)
 * wire up correctly without any external infrastructure.</p>
 *
 * <p>{@link DynamicPropertySource} points {@code gateway.jwt.public-key-path}
 * at a temp file containing a freshly generated RSA-2048 public key. This
 * avoids the context failing because the real auth-service key doesn't exist
 * in the test classpath.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // Don't try to fetch config from a (possibly-missing) Config Server
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.config.import=",
                // Don't try to register with (possibly-missing) Eureka
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                // Provide a minimal in-line route so the gateway actually has something
                "spring.cloud.gateway.routes[0].id=test-route",
                "spring.cloud.gateway.routes[0].uri=http://example.com",
                "spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**"
        })
@ActiveProfiles("local")
class ApiGatewayApplicationTests {

    /**
     * Provide a generated test public key before the Spring context starts.
     * GatewaySecurityConfig calls PemKeyLoader with this path to create the
     * RSAPublicKey bean — so the context boots without needing the real key file.
     */
    @DynamicPropertySource
    static void overrideJwtPublicKeyPath(DynamicPropertyRegistry registry) {
        registry.add("gateway.jwt.public-key-path",
                () -> TestPublicKey.publicKeyPath().toString());
    }

    @Test
    void contextLoads() {
        // @SpringBootTest fails the test if the context can't start.
    }
}
