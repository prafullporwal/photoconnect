package com.photoconnect.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test for the gateway's Spring context.
 *
 * <p>We disable Config Server and Eureka in this test so the context can boot
 * standalone — the test isn't trying to verify those integrations, only that
 * the gateway's own beans (routes, filters, etc.) wire up correctly.</p>
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

    @Test
    void contextLoads() {
        // @SpringBootTest fails the test if the context can't start.
    }
}
