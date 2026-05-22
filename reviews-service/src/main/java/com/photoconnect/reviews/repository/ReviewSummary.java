package com.photoconnect.reviews.repository;

/**
 * Aggregate projection: average rating + count for a single photographer.
 *
 * <p>This is a Spring Data <em>interface projection</em> — the JPA provider
 * returns a runtime proxy that reads each column by its alias. Cheaper than
 * loading every {@link com.photoconnect.reviews.domain.Review} row just to
 * count and average.</p>
 *
 * <p>Important: {@code averageRating} is a {@code Double}, not a primitive,
 * because the query returns {@code NULL} when there are no reviews. The
 * service layer translates that to "0.0 + 0 reviews".</p>
 */
public interface ReviewSummary {
    Double getAverageRating();
    long getReviewCount();
}
