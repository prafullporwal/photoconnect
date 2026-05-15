package com.photoconnect.auth;

import com.photoconnect.auth.testutil.TestKeyPair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;

/**
 * Full-context smoke test for auth-service. Brings up:
 *   - A real PostgreSQL container (Testcontainers) wired via @ServiceConnection
 *   - Disabled Config Server / Eureka / Redis autoconfig
 *   - Fresh RSA keys written to a temp dir before context start
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.config.import=",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "auth.jwt.issuer=photoconnect-test",
                "auth.jwt.audience=photoconnect-api-test",
                "auth.jwt.access-token-ttl=PT15M",
                "auth.jwt.refresh-token-ttl=P7D"
        })
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("local")
class AuthServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_db_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @TempDir
    static Path keyDir;

    @DynamicPropertySource
    static void generateKeys(DynamicPropertyRegistry registry) throws Exception {
        TestKeyPair.Paths paths = TestKeyPair.writeNew(keyDir);
        registry.add("auth.jwt.private-key-path", () -> paths.privateKey().toString());
        registry.add("auth.jwt.public-key-path",  () -> paths.publicKey().toString());
    }

    @Test
    void contextLoads() {
        // Boot succeeds with real Postgres + Flyway migrations + RSA keys.
    }
}
