package com.photoconnect.photographer;

import com.photoconnect.photographer.testutil.TestKeyPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

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

    @TempDir
    static Path keyDir;

    /**
     * Generate a fresh RSA public key so the {@code serviceJwtPublicKey} bean
     * can be created during context refresh. The key isn't exercised by this
     * test — we only need a real PEM file at the configured path.
     */
    @DynamicPropertySource
    static void serviceJwtKey(DynamicPropertyRegistry registry) throws Exception {
        Path pub = TestKeyPair.writePublicKey(keyDir);
        registry.add("photographer.service-jwt.public-key-path", pub::toString);
    }

    @Test
    void contextLoads() {
        // @SpringBootTest fails if the context can't start.
    }
}
