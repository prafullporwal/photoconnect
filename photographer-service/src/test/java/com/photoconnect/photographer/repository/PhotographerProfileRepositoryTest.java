package com.photoconnect.photographer.repository;

import com.photoconnect.photographer.domain.PhotographerProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test using a real PostgreSQL container.
 *
 * <p>We use {@code @Testcontainers(disabledWithoutDocker = true)} so the test
 * is silently skipped when Docker is not available (e.g. some CI agents),
 * rather than failing the build. {@code @ServiceConnection} wires the container's
 * JDBC URL, username, and password into {@code spring.datasource.*} automatically
 * — no {@code @DynamicPropertySource} boilerplate needed.</p>
 *
 * <p>{@code @AutoConfigureTestDatabase(replace = NONE)} prevents Spring Boot
 * from substituting an in-memory H2 database. We want the real PostgreSQL DDL
 * because Flyway runs the V1 migration against the container on first start.</p>
 */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        // @DataJpaTest defaults to validate; Flyway handles the schema, so keep validate
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PhotographerProfileRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    PhotographerProfileRepository repository;

    @Test
    void save_andFindByUserId() {
        UUID userId = UUID.randomUUID();
        PhotographerProfile profile = buildProfile(userId, true);

        PhotographerProfile saved = repository.save(profile);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();   // @PrePersist fired
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(repository.findByUserId(userId))
                .isPresent()
                .get()
                .extracting(PhotographerProfile::getDisplayName)
                .isEqualTo("Alice Photography");
    }

    @Test
    void existsByUserId_trueForExisting_falseForNew() {
        UUID userId = UUID.randomUUID();
        repository.save(buildProfile(userId, true));

        assertThat(repository.existsByUserId(userId)).isTrue();
        assertThat(repository.existsByUserId(UUID.randomUUID())).isFalse();
    }

    @Test
    void findAllByAvailableTrue_excludesUnavailableProfiles() {
        repository.save(buildProfile(UUID.randomUUID(), true));   // visible
        repository.save(buildProfile(UUID.randomUUID(), false));  // hidden

        List<PhotographerProfile> visible = repository.findAllByAvailableTrue();

        assertThat(visible).hasSize(1);
        assertThat(visible.get(0).isAvailable()).isTrue();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static PhotographerProfile buildProfile(UUID userId, boolean available) {
        PhotographerProfile p = new PhotographerProfile();
        p.setUserId(userId);
        p.setDisplayName("Alice Photography");
        p.setBio("Wedding specialist.");
        p.setLocation("New York, NY");
        p.setYearsOfExperience(10);
        p.setPricePerHour(new BigDecimal("200.00"));
        p.setAvailable(available);
        p.setSpecialties(List.of("wedding", "portrait"));
        return p;
    }
}
