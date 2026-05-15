package com.photoconnect.photographer.repository;

import com.photoconnect.photographer.domain.MediaType;
import com.photoconnect.photographer.domain.PortfolioItem;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, UUID> {

    /** All items for a profile, sorted for display. */
    List<PortfolioItem> findByPhotographerProfileIdOrderByDisplayOrderAscUploadedAtAsc(UUID profileId);

    /** Filtered listing — used by the gallery's "show me only wedding videos" view. */
    List<PortfolioItem> findByPhotographerProfileIdAndMediaTypeOrderByDisplayOrderAscUploadedAtAsc(
            UUID profileId, MediaType mediaType);

    List<PortfolioItem> findByPhotographerProfileIdAndCategoryOrderByDisplayOrderAscUploadedAtAsc(
            UUID profileId, String category);

    /**
     * Marketplace feed — portfolio items across all <em>available</em>
     * photographers, newest first. Each row is denormalised with the
     * photographer's name + location so the SPA can render tiles in one round trip.
     *
     * <p>This is the photographer-service equivalent of a Pinterest "explore"
     * query — we deliberately avoid a JPA association on {@code PortfolioItem}
     * (no {@code @ManyToOne PhotographerProfile}) because the link is logical,
     * not foreign-keyed across services. The JOIN here is an ad-hoc JPQL
     * cross-entity match by UUID.</p>
     */
    @Query("""
        SELECT new com.photoconnect.photographer.repository.FeedRow(
            pi.id, pi.mediaType, pi.category, pi.mimeType, pi.publicUrl, pi.uploadedAt,
            pp.id, pp.displayName, pp.location)
        FROM PortfolioItem pi
        JOIN com.photoconnect.photographer.domain.PhotographerProfile pp
          ON pp.id = pi.photographerProfileId
        WHERE pp.available = true
        ORDER BY pi.uploadedAt DESC
        """)
    List<FeedRow> findFeed(Limit limit);
}
