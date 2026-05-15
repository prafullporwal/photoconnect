package com.photoconnect.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Audit columns reused by every entity that should track who/when.
 *
 * <p>{@code @CreatedDate} / {@code @LastModifiedDate} are populated by
 * Spring Data's {@link AuditingEntityListener} (enabled by
 * {@code @EnableJpaAuditing} on the main app class).</p>
 *
 * <p>{@code createdBy} / {@code updatedBy} come from an {@code AuditorAware}
 * bean we'll add in {@code AuditConfig} — it pulls the current user from
 * the {@code SecurityContext}.</p>
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 255)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}
