package com.saryom.foodservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FoodPostRepository extends JpaRepository<FoodPost, UUID> {

    /**
     * Browsable = still available and not past its best-before time, optionally
     * narrowed by a case-insensitive title match and/or food type. Used for the
     * straight paged/sorted feed when no coordinates are supplied.
     */
    @Query("""
            SELECT f FROM FoodPost f
            WHERE f.status = com.saryom.foodservice.domain.FoodStatus.AVAILABLE
              AND (f.bestBefore IS NULL OR f.bestBefore > :now)
              AND (:q IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
              AND (:type IS NULL OR f.foodType = :type)
            """)
    Page<FoodPost> browse(@Param("q") String q, @Param("type") FoodType type,
                          @Param("now") Instant now, Pageable pageable);

    /** Same filter as {@link #browse} but unpaged — the geo path sorts/paginates by distance in memory. */
    @Query("""
            SELECT f FROM FoodPost f
            WHERE f.status = com.saryom.foodservice.domain.FoodStatus.AVAILABLE
              AND (f.bestBefore IS NULL OR f.bestBefore > :now)
              AND (:q IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
              AND (:type IS NULL OR f.foodType = :type)
              AND f.lat IS NOT NULL AND f.lng IS NOT NULL
            """)
    List<FoodPost> browseWithCoordinates(@Param("q") String q, @Param("type") FoodType type,
                                         @Param("now") Instant now);

    List<FoodPost> findByGiverIdOrderByCreatedAtDesc(String giverId);

    List<FoodPost> findByClaimedByAndStatusOrderByClaimedAtDesc(String claimedBy, FoodStatus status);

    /**
     * Reservations held past their collection window, oldest first.
     *
     * <p>Paged so one sweep cannot load an unbounded backlog into memory — if a
     * long outage leaves thousands stale, they drain over several runs instead of
     * one enormous transaction.
     */
    Page<FoodPost> findByStatusAndClaimedAtBeforeOrderByClaimedAtAsc(
            FoodStatus status, Instant claimedBefore, Pageable pageable);
}
