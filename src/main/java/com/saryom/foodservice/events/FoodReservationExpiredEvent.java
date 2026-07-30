package com.saryom.foodservice.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a reservation is released automatically because it was never
 * collected. Topic {@code food.reservation_expired}.
 *
 * <p>Carries both parties: the taker needs to know they lost the reservation
 * (otherwise they turn up to nothing), and the giver needs to know their food is
 * back in the pool.
 */
public record FoodReservationExpiredEvent(
        UUID eventId,
        Instant occurredAt,
        UUID foodPostId,
        String giverId,
        String previousClaimerId,
        String title) {

    public static FoodReservationExpiredEvent of(UUID foodPostId, String giverId,
                                                 String previousClaimerId, String title) {
        return new FoodReservationExpiredEvent(UUID.randomUUID(), Instant.now(),
                foodPostId, giverId, previousClaimerId, title);
    }
}
