package com.photoconnect.auth.repository;

import com.photoconnect.auth.domain.Role;
import com.photoconnect.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest slice against a real PostgreSQL container via Testcontainers.
 *
 * <p>Flyway runs against this container on context startup, so the test
 * also implicitly verifies that V1/V2 migrations apply cleanly.</p>
 *
 * <p>{@code AutoConfigureTestDatabase(replace = NONE)} keeps Spring Boot
 * from substituting an embedded H2 — we want the real engine.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(com.photoconnect.auth.config.AuditConfig.class)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_db_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    UserRepository userRepository;

    @Test
    void findByEmailIgnoreCaseAndDeletedAtIsNull_returnsActiveUser() {
        User saved = userRepository.save(User.builder()
                .email("alice@example.com")
                .passwordHash("$2a$12$irrelevant")
                .role(Role.PHOTOGRAPHER)
                .enabled(true)
                .build());

        Optional<User> found = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("ALICE@EXAMPLE.COM");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByEmailIgnoreCaseAndDeletedAtIsNull_skipsSoftDeleted() {
        User saved = userRepository.save(User.builder()
                .email("bob@example.com")
                .passwordHash("$2a$12$irrelevant")
                .role(Role.CUSTOMER)
                .enabled(true)
                .deletedAt(Instant.now())
                .build());

        Optional<User> found = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("bob@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmailIgnoreCaseAndDeletedAtIsNull_works() {
        userRepository.save(User.builder()
                .email("carol@example.com")
                .passwordHash("$2a$12$irrelevant")
                .role(Role.PHOTOGRAPHER)
                .enabled(true)
                .build());

        assertThat(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("CAROL@example.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("not-here@example.com")).isFalse();
    }
}
