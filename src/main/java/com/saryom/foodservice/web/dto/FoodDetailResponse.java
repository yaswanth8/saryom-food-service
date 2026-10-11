package com.saryom.foodservice.web.dto;

import com.saryom.foodservice.domain.FoodPost;
import com.saryom.foodservice.domain.FoodStatus;
import com.saryom.foodservice.service.ReservationPolicy;

import java.time.Instant;
import java.util.UUID;

/** Full food view for the detail screen. Includes viewer-relative flags. */
public record FoodDetailResponse(
        UUID id,
        String giverId,
        String title,
        String description,
        String quantity,
        String foodType,
        String dietary,
        String locationText,
        Double lat,
        Double lng,
        Instant bestBefore,
        String imageUrl,
        String status,
        boolean mine,
        boolean reservedByMe,
        Instant createdAt,
        /**
         * While RESERVED, when the hold lapses and the food goes back up for
         * anyone; null in every other state. Shown to the taker as "collect by",
         * to the giver as when it returns, and to everyone else as when it might
         * become available again. Appended last so older clients ignore it.
         */
        Instant reservedUntil) {

    public static FoodDetailResponse from(FoodPost f, String viewerId, ReservationPolicy reservations) {
        return new FoodDetailResponse(f.getId(), f.getGiverId(), f.getTitle(), f.getDescription(),
                f.getQuantity(), f.getFoodType().name(), f.getDietary().name(), f.getLocationText(),
                f.getLat(), f.getLng(), f.getBestBefore(), f.getImageUrl(), f.getStatus().name(),
                f.isOwnedBy(viewerId == null ? "" : viewerId), f.isClaimedBy(viewerId), f.getCreatedAt(),
                f.getStatus() == FoodStatus.RESERVED && f.getClaimedAt() != null
                        ? reservations.expiresAt(f.getClaimedAt()) : null);
    }
}
