package com.photoconnect.reviews.repository;

import com.photoconnect.reviews.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /** Reviews shown on a photographer's public profile, newest first. */
    List<Review> findByPhotographerProfileIdOrderByCreatedAtDesc(UUID photographerProfileId);

    /** "My reviews" tab for the authoring customer. */
    List<Review> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    /** Fast existence check used to translate UNIQUE-violation races. */
    boolean existsByCustomerIdAndPhotographerProfileId(UUID customerId, UUID photographerProfileId);

    /**
     * Aggregate rating + review count for a single photographer profile.
     *
     * <p>JPQL aliases (AS averageRating / AS reviewCount) MUST match the
     * getter names on {@link ReviewSummary} — Spring Data uses bean-property
     * naming on the interface projection.</p>
     */
    @Query("""
            select avg(cast(r.rating as double)) as averageRating,
                   count(r)                       as reviewCount
            from   Review r
            where  r.photographerProfileId = :profileId
            """)
    ReviewSummary summarise(@Param("profileId") UUID photographerProfileId);
}
