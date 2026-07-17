package com.saryom.foodservice.web.dto;

import com.saryom.foodservice.domain.FoodPost;

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
        Instant createdAt) {

    public static FoodDetailResponse from(FoodPost f, String viewerId) {
        return new FoodDetailResponse(f.getId(), f.getGiverId(), f.getTitle(), f.getDescription(),
                f.getQuantity(), f.getFoodType().name(), f.getDietary().name(), f.getLocationText(),
                f.getLat(), f.getLng(), f.getBestBefore(), f.getImageUrl(), f.getStatus().name(),
                f.isOwnedBy(viewerId == null ? "" : viewerId), f.isClaimedBy(viewerId), f.getCreatedAt());
    }
}
