package com.saryom.foodservice.events;

import java.time.Instant;
import java.util.UUID;

/**
 * The food actually changed hands. Topic {@code food.completed}.
 *
 * <p>Distinct from {@code food.claimed}, which only says someone reserved it —
 * plenty of reservations are released or quietly expire without the two people
 * ever meeting. Only this event records that a giver and a collector completed
 * a handover, which is what makes a review of either of them earned rather than
 * merely asserted.
 *
 * <p>{@code collectorId} is always present: only a RESERVED post can be
 * completed, so there is by construction someone who claimed it. Consumers may
 * rely on that rather than guarding for a missing counterparty.
 */
public record FoodCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID foodPostId,
        String giverId,
        String collectorId,
        String title) {

    public static FoodCompletedEvent of(UUID foodPostId, String giverId, String collectorId, String title) {
        return new FoodCompletedEvent(UUID.randomUUID(), Instant.now(),
                foodPostId, giverId, collectorId, title);
    }
}
