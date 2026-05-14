package com.photoconnect.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verify that the Spring application context loads successfully
 * with the Eureka server auto-configuration enabled.
 *
 * <p>Why a "does the context load" test? It's the cheapest possible insurance
 * against silly mistakes (missing dependency, typo in application.yml, broken
 * autoconfiguration). It runs in seconds and catches 90% of merge-conflict
 * issues before they hit CI.</p>
 *
 * <p>We bind to a random port so this test can run concurrently with a real
 * Eureka instance already running on 8761.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // Don't try to join a cluster during tests
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false"
        })
@ActiveProfiles("local")
class DiscoveryServiceApplicationTests {

    @Test
    void contextLoads() {
        // No assertion needed — @SpringBootTest fails the test if the context
        // can't be created.
    }
}
