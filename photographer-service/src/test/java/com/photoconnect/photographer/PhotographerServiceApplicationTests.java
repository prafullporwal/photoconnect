package com.photoconnect.photographer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring context loads successfully.
 *
 * <p>Config Server, Eureka, and the real database are all disabled so this
 * test runs offline. The fallback values in {@code application.properties}
 * satisfy all {@code @Validated} configuration properties.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.config.import=",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                // Use H2 in-memory for the context load test (no Flyway, no Postgres)
                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@ActiveProfiles("local")
class PhotographerServiceApplicationTests {

    @Test
    void contextLoads() {
        // @SpringBootTest fails if the context can't start.
    }
}
