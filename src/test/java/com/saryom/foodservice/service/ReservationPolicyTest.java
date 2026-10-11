package com.saryom.foodservice.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationPolicyTest {

    private static final Instant CLAIMED = Instant.parse("2026-10-11T17:00:00Z");

    @Test
    void theDeadlineIsTheHoldAfterTheClaim() {
        assertThat(new ReservationPolicy(6).expiresAt(CLAIMED)).isEqualTo(CLAIMED.plus(Duration.ofHours(6)));
    }

    @Test
    void theShownDeadlineAndTheSweepersCutoffAgree() {
        // The reason this class exists. The sweeper releases a reservation once
        // its claim is older than staleBefore(now); the page tells the taker
        // expiresAt(claim). A gap between the two would mean food released while
        // the page still promised it, or a promise the sweeper never honours.
        ReservationPolicy policy = new ReservationPolicy(24);
        Instant deadline = policy.expiresAt(CLAIMED);

        // A moment before the shown deadline the sweeper must not release it…
        Instant justBefore = deadline.minusSeconds(1);
        assertThat(CLAIMED.isBefore(policy.staleBefore(justBefore))).isFalse();
        // …and a moment after, it must.
        Instant justAfter = deadline.plusSeconds(1);
        assertThat(CLAIMED.isBefore(policy.staleBefore(justAfter))).isTrue();
    }

    @Test
    void aNonPositiveHoldIsRefusedAtStartup() {
        // Zero would release every reservation on the next sweep.
        assertThatThrownBy(() -> new ReservationPolicy(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReservationPolicy(-3)).isInstanceOf(IllegalArgumentException.class);
    }
}
