package com.saryom.foodservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * How long a taker may hold food before it goes back to the pool.
 *
 * <p>One place for it, because two things depend on it and must never disagree:
 * the sweeper that releases stale reservations, and the "collect by" deadline
 * shown to the person holding one. Had the API computed its own deadline, an
 * operator shortening FOOD_RESERVATION_HOLD_HOURS would leave every displayed
 * deadline promising time the sweeper no longer gives.
 */
@Component
public class ReservationPolicy {

    private final Duration hold;

    public ReservationPolicy(@Value("${saryom.food.reservation-hold-hours:24}") long holdHours) {
        if (holdHours <= 0) {
            // Zero would release every reservation on the next sweep, which
            // looks like a bug to every user who reserves anything.
            throw new IllegalArgumentException("reservation-hold-hours must be positive, was " + holdHours);
        }
        this.hold = Duration.ofHours(holdHours);
    }

    public Duration hold() {
        return hold;
    }

    /**
     * When a reservation made at {@code claimedAt} stops being guaranteed.
     *
     * <p>The sweeper runs on an interval, so the food may in practice stay held
     * a little past this — but never less. Showing the earliest moment it can be
     * lost is the promise worth making.
     */
    public Instant expiresAt(Instant claimedAt) {
        return claimedAt.plus(hold);
    }

    /** Reservations claimed before this instant are due for release. */
    public Instant staleBefore(Instant now) {
        return now.minus(hold);
    }
}
