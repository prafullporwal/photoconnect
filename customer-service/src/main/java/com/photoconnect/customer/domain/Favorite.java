package com.photoconnect.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer's bookmark / "saved photographer".
 *
 * <h2>Why no {@code @Version}?</h2>
 * <p>Favorites are immutable: you either have one or you don't. The only
 * mutations are <em>insert</em> (save) and <em>delete</em> (unsave). With no
 * field updates there's no lost-update concern, so optimistic locking
 * would be ceremony for its own sake.</p>
 *
 * <h2>What we store</h2>
 * <p>Just the portfolio item ID — everything else (media URL, category,
 * photographer name) is fetched from photographer-service at read time
 * and embedded in the response. We don't denormalise here because the
 * item metadata can change (rename category, re-upload) and we want
 * the saved page to always show current data.</p>
 */
@Entity
@Table(name = "favorites")
@Getter
@Setter
@NoArgsConstructor
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private UUID id;

    /** auth-service userId of the customer who saved this content. */
    @Column(name = "customer_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID customerId;

    /** PortfolioItem PK in photographer-service. */
    @Column(name = "portfolio_item_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID portfolioItemId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        createdAt = Instant.now();
    }
}
