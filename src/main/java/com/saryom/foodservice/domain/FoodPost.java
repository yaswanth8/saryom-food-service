package com.saryom.foodservice.domain;

import com.saryom.foodservice.error.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.UUID;

/**
 * A post offering surplus/free food. Aggregate root: the reserve/release/complete/
 * cancel lifecycle is encapsulated here so invalid transitions are impossible from
 * the outside (rich domain model — the service orchestrates, the entity enforces
 * its own invariants).
 */
@Entity
@Table(name = "food_post")
public class FoodPost {

    @Id
    private UUID id;

    /**
     * Guards the reservation lifecycle against lost updates. Two concurrent
     * reserve calls both see AVAILABLE and both pass {@link #reserve}'s guard;
     * without a version the later write wins silently and two takers each think
     * they claimed the food. With it, the loser's commit fails and the service
     * turns that into a 409.
     */
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "giver_id", nullable = false)
    private String giverId;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    /** Free-text amount, e.g. "3 portions", "half a loaf". */
    @Column(nullable = false)
    private String quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", nullable = false)
    private FoodType foodType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DietaryType dietary;

    @Column(name = "location_text")
    private String locationText;

    private Double lat;

    private Double lng;

    /** Collect-by time; posts past this are hidden from browse. Nullable = no deadline. */
    @Column(name = "best_before")
    private Instant bestBefore;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodStatus status;

    @Column(name = "claimed_by")
    private String claimedBy;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FoodPost() {
        // JPA
    }

    public FoodPost(UUID id, String giverId, String title, String description, String quantity,
                    FoodType foodType, DietaryType dietary, String locationText, Double lat, Double lng,
                    Instant bestBefore, String imageUrl, Instant now) {
        this.id = id;
        this.giverId = giverId;
        this.title = title;
        this.description = description;
        this.quantity = quantity;
        this.foodType = foodType;
        this.dietary = dietary == null ? DietaryType.UNSPECIFIED : dietary;
        this.locationText = locationText;
        this.lat = lat;
        this.lng = lng;
        this.bestBefore = bestBefore;
        this.imageUrl = imageUrl;
        this.status = FoodStatus.AVAILABLE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Edit details. Only permitted while the post is still available (not reserved/done). */
    public void update(String title, String description, String quantity, FoodType foodType,
                       DietaryType dietary, String locationText, Instant bestBefore, String imageUrl,
                       Instant now) {
        if (status != FoodStatus.AVAILABLE) {
            throw new ConflictException("Only an available post can be edited");
        }
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (quantity != null) this.quantity = quantity;
        if (foodType != null) this.foodType = foodType;
        if (dietary != null) this.dietary = dietary;
        if (locationText != null) this.locationText = locationText;
        if (bestBefore != null) this.bestBefore = bestBefore;
        if (imageUrl != null) this.imageUrl = imageUrl;
        this.updatedAt = now;
    }

    /** A taker reserves the food. The giver cannot reserve their own post. */
    public void reserve(String takerId, Instant now) {
        if (status != FoodStatus.AVAILABLE) {
            throw new ConflictException("This food is no longer available");
        }
        if (isExpired(now)) {
            throw new ConflictException("This food post has expired");
        }
        if (giverId.equals(takerId)) {
            throw new IllegalArgumentException("You cannot reserve your own food post");
        }
        this.status = FoodStatus.RESERVED;
        this.claimedBy = takerId;
        this.claimedAt = now;
        this.updatedAt = now;
    }

    /** Release a reservation back to available (by the taker who reserved, or the giver). */
    public void release(Instant now) {
        if (status != FoodStatus.RESERVED) {
            throw new ConflictException("This post is not reserved");
        }
        this.status = FoodStatus.AVAILABLE;
        this.claimedBy = null;
        this.claimedAt = null;
        this.updatedAt = now;
    }

    /** The giver confirms the food was collected. */
    public void complete(Instant now) {
        if (status != FoodStatus.RESERVED) {
            throw new ConflictException("Only a reserved post can be marked collected");
        }
        this.status = FoodStatus.COMPLETED;
        this.updatedAt = now;
    }

    /** The giver withdraws the post. */
    public void cancel(Instant now) {
        if (status == FoodStatus.COMPLETED || status == FoodStatus.CANCELLED) {
            throw new ConflictException("This post is already closed");
        }
        this.status = FoodStatus.CANCELLED;
        this.updatedAt = now;
    }

    public boolean isExpired(Instant now) {
        return bestBefore != null && bestBefore.isBefore(now);
    }

    public boolean isOwnedBy(String uid) {
        return giverId.equals(uid);
    }

    public boolean isClaimedBy(String uid) {
        return uid != null && uid.equals(claimedBy);
    }

    /** Guard: throws 403 unless {@code uid} is the giver. */
    public void requireOwner(String uid) {
        if (!isOwnedBy(uid)) {
            throw new AccessDeniedException("Only the giver may modify this food post");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getGiverId() {
        return giverId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getQuantity() {
        return quantity;
    }

    public FoodType getFoodType() {
        return foodType;
    }

    public DietaryType getDietary() {
        return dietary;
    }

    public String getLocationText() {
        return locationText;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public Instant getBestBefore() {
        return bestBefore;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public FoodStatus getStatus() {
        return status;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
