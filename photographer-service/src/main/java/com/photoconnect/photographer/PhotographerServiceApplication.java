package com.photoconnect.photographer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PhotoConnect — Photographer Service entry point.
 *
 * <p>This service owns photographer profiles: who you are, where you're based,
 * what you specialise in, and what you charge. It is <em>not</em> auth — it
 * trusts the identity headers ({@code X-User-Id}, {@code X-User-Role}) that
 * api-gateway stamps on every validated request.</p>
 *
 * <p>Persistence: {@code photographer_db} on PostgreSQL. Schema is owned by
 * Flyway; Hibernate only validates.</p>
 *
 * <p>Note: no {@code @EnableJpaAuditing} here. Because this service uses
 * {@code @PrePersist} / {@code @PreUpdate} lifecycle callbacks for timestamps
 * instead of Spring Data's {@code AuditingEntityListener}, no auditing
 * infrastructure is needed. This avoids the {@code @WebMvcTest} gotcha that
 * auth-service ran into.</p>
 */
@SpringBootApplication
public class PhotographerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotographerServiceApplication.class, args);
    }
}
