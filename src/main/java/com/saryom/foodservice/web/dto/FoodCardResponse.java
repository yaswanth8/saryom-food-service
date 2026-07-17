package com.saryom.foodservice.web.dto;

import com.saryom.foodservice.domain.FoodPost;

import java.time.Instant;
import java.util.UUID;

/** Compact food view for the feed grid. */
public record FoodCardResponse(
        UUID id,
        String title,
        String quantity,
        String foodType,
        String dietary,
        String locationText,
        Double distanceMiles,
        Instant bestBefore,
        String imageUrl,
        String status,
        Instant createdAt) {

    public static FoodCardResponse from(FoodPost f, Double distanceMiles) {
        return new FoodCardResponse(f.getId(), f.getTitle(), f.getQuantity(), f.getFoodType().name(),
                f.getDietary().name(), f.getLocationText(), distanceMiles, f.getBestBefore(),
                f.getImageUrl(), f.getStatus().name(), f.getCreatedAt());
    }
}
