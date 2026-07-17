package com.saryom.foodservice.events;

import java.time.Instant;
import java.util.UUID;

/** Emitted when surplus food is shared. Topic {@code food.posted}. */
public record FoodPostedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID foodPostId,
        String giverId,
        String title,
        String foodType,
        String locationText) {

    public static FoodPostedEvent of(UUID foodPostId, String giverId, String title,
                                     String foodType, String locationText) {
        return new FoodPostedEvent(UUID.randomUUID(), Instant.now(),
                foodPostId, giverId, title, foodType, locationText);
    }
}
