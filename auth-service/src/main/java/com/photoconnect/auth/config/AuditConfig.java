package com.photoconnect.auth.config;

import com.photoconnect.auth.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Tells Spring Data JPA Auditing how to find the current actor for
 * {@code @CreatedBy} / {@code @LastModifiedBy}, and turns on auditing itself.
 *
 * <p>Keeping {@code @EnableJpaAuditing} on this {@code @Configuration} class
 * (rather than the main app class) means non-JPA test slices like
 * {@code @WebMvcTest} don't drag in audit infrastructure that needs JPA
 * entities to exist.</p>
 *
 * <p>We use the user's ID (UUID) as the audit string. If nobody is
 * authenticated (register, scheduled jobs, …), we fall back to "system".</p>
 */
@Configuration
@EnableJpaAuditing
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
                return Optional.of("system");
            }
            if (auth.getPrincipal() instanceof UserPrincipal up) {
                return Optional.of(up.userId().toString());
            }
            return Optional.of(auth.getName());
        };
    }
}
