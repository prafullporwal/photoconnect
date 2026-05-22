package com.photoconnect.customer.repository;

import com.photoconnect.customer.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    Optional<Favorite> findByCustomerIdAndPortfolioItemId(UUID customerId, UUID portfolioItemId);

    boolean existsByCustomerIdAndPortfolioItemId(UUID customerId, UUID portfolioItemId);

    List<Favorite> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Favorite f WHERE f.customerId = :customerId " +
            "AND f.portfolioItemId = :portfolioItemId")
    int deleteByCustomerAndPortfolioItem(UUID customerId, UUID portfolioItemId);
}
