package com.photoconnect.reviews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the Spring context must load without errors.
 *
 * <p>Runs against the {@code test} profile so the app does NOT try to reach
 * PostgreSQL, Eureka, or the Config Server at startup. Those are all overridden
 * with lightweight in-process stubs configured in
 * {@code src/test/resources/application-test.yml}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ReviewsServiceApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to start, this test fails with a helpful
        // BeanCreationException rather than a silent "0 tests found" result.
    }
}
