package com.saryom.foodservice.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a taker reserves shared food. Topic {@code food.claimed}. Carries
 * both parties so the notification service can alert the giver that someone is
 * coming to collect.
 */
public record FoodClaimedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID foodPostId,
        String giverId,
        String claimerId,
        String title) {

    public static FoodClaimedEvent of(UUID foodPostId, String giverId, String claimerId, String title) {
        return new FoodClaimedEvent(UUID.randomUUID(), Instant.now(),
                foodPostId, giverId, claimerId, title);
    }
}
