package com.saryom.foodservice.service;

import com.saryom.foodservice.domain.DietaryType;
import com.saryom.foodservice.domain.FoodPost;
import com.saryom.foodservice.domain.FoodPostRepository;
import com.saryom.foodservice.domain.FoodStatus;
import com.saryom.foodservice.domain.FoodType;
import com.saryom.foodservice.events.DomainEventPublisher;
import com.saryom.foodservice.events.FoodReservationExpiredEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationSweeperTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final FoodPostRepository posts = mock(FoodPostRepository.class);
    private final DomainEventPublisher events = mock(DomainEventPublisher.class);
    private final FoodService foodService = new FoodService(posts, events, CLOCK);
    private final ReservationSweeper sweeper = new ReservationSweeper(posts, foodService, CLOCK, 24);

    private FoodPost reservedPost(String taker, Instant claimedAt) {
        FoodPost post = new FoodPost(UUID.randomUUID(), "giver", "Soup", "warm", "2 portions",
                FoodType.PREPARED_MEAL, DietaryType.UNSPECIFIED, "Fremont, CA", null, null,
                null, null, claimedAt);
        post.reserve(taker, claimedAt);
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(posts.save(any(FoodPost.class))).thenAnswer(i -> i.getArgument(0));
        return post;
    }

    private void staleRowsAre(FoodPost... stale) {
        when(posts.findByStatusAndClaimedAtBeforeOrderByClaimedAtAsc(
                eq(FoodStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stale)));
    }

    /**
     * The whole point: a reservation nobody collected used to hold the post in
     * RESERVED forever — hidden from browse and un-reservable — so perishable
     * food was guaranteed to be wasted.
     */
    @Test
    void releasesAReservationHeldPastTheWindow() {
        FoodPost stale = reservedPost("bob", NOW.minusSeconds(25 * 3600));
        staleRowsAre(stale);

        assertThat(sweeper.sweep()).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(FoodStatus.AVAILABLE);
        assertThat(stale.getClaimedBy()).isNull();
    }

    /** Both parties need telling — the taker so they do not turn up to nothing. */
    @Test
    void announcesTheExpiryWithBothParties() {
        FoodPost stale = reservedPost("bob", NOW.minusSeconds(25 * 3600));
        staleRowsAre(stale);

        sweeper.sweep();

        var captor = org.mockito.ArgumentCaptor.forClass(FoodReservationExpiredEvent.class);
        verify(events).publish(eq("food.reservation_expired"), captor.capture());
        assertThat(captor.getValue().previousClaimerId()).isEqualTo("bob");
        assertThat(captor.getValue().giverId()).isEqualTo("giver");
    }

    /**
     * Reflects losing a race — the taker collected it, someone released it, or a
     * sweeper on another instance won. None is an error.
     */
    @Test
    void reportsNoOpWhenThePostIsNoLongerReserved() {
        FoodPost collected = reservedPost("bob", NOW.minusSeconds(25 * 3600));
        collected.complete(NOW);
        staleRowsAre(collected);

        assertThat(sweeper.sweep()).isZero();
        verify(events, never()).publish(eq("food.reservation_expired"), any());
    }

    @Test
    void reportsNoOpWhenThePostVanished() {
        FoodPost gone = reservedPost("bob", NOW.minusSeconds(25 * 3600));
        when(posts.findById(gone.getId())).thenReturn(Optional.empty());
        staleRowsAre(gone);

        assertThat(sweeper.sweep()).isZero();
    }

    /** One bad row must not abandon the rest of the batch. */
    @Test
    void continuesThroughAFailingRow() {
        FoodPost bad = reservedPost("bob", NOW.minusSeconds(25 * 3600));
        FoodPost good = reservedPost("carol", NOW.minusSeconds(26 * 3600));
        when(posts.findById(bad.getId())).thenThrow(new IllegalStateException("boom"));
        staleRowsAre(bad, good);

        assertThat(sweeper.sweep()).isEqualTo(1);
        assertThat(good.getStatus()).isEqualTo(FoodStatus.AVAILABLE);
    }

    @Test
    void doesNothingWhenNoReservationIsStale() {
        staleRowsAre();

        assertThat(sweeper.sweep()).isZero();
        verify(events, never()).publish(any(), any());
    }
}
