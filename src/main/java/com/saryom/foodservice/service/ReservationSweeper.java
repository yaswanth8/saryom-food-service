package com.saryom.foodservice.service;

import com.saryom.foodservice.domain.FoodPost;
import com.saryom.foodservice.domain.FoodPostRepository;
import com.saryom.foodservice.domain.FoodStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Returns uncollected food to the pool.
 *
 * <p>A reservation had no expiry, so a taker who reserved and never turned up
 * held the post in RESERVED forever: hidden from browse, un-reservable by anyone
 * else, and — for perishable food — guaranteed to be wasted. Which is the exact
 * outcome the whole service exists to prevent.
 *
 * <p>Safe to run on more than one instance. Each release goes through
 * {@link FoodService#expireReservation}, which re-checks the status inside its
 * own transaction, and {@code FoodPost} carries a {@code @Version} column, so a
 * concurrent sweeper simply loses the write and reports no-op rather than
 * double-releasing.
 */
@Component
public class ReservationSweeper {

    private static final Logger log = LoggerFactory.getLogger(ReservationSweeper.class);

    /** Bounds one sweep. A large backlog drains over successive runs. */
    private static final int BATCH_SIZE = 200;

    private final FoodPostRepository posts;
    private final FoodService foodService;
    private final Clock clock;
    private final Duration holdDuration;

    public ReservationSweeper(FoodPostRepository posts,
                              FoodService foodService,
                              Clock clock,
                              @Value("${saryom.food.reservation-hold-hours:24}") long holdHours) {
        this.posts = posts;
        this.foodService = foodService;
        this.clock = clock;
        this.holdDuration = Duration.ofHours(holdHours);
    }

    /**
     * Fixed delay rather than a fixed rate: runs never overlap, and a slow sweep
     * cannot queue up behind itself.
     */
    @Scheduled(fixedDelayString = "${saryom.food.sweep-interval-ms:900000}",
            initialDelayString = "${saryom.food.sweep-initial-delay-ms:60000}")
    public void releaseStaleReservations() {
        int released = sweep();
        if (released > 0) {
            log.info("Released {} uncollected reservation(s) held longer than {}h",
                    released, holdDuration.toHours());
        }
    }

    /** Exposed for tests; returns how many reservations this sweep released. */
    int sweep() {
        List<FoodPost> stale = posts.findByStatusAndClaimedAtBeforeOrderByClaimedAtAsc(
                        FoodStatus.RESERVED,
                        clock.instant().minus(holdDuration),
                        PageRequest.of(0, BATCH_SIZE))
                .getContent();

        int released = 0;
        for (FoodPost post : stale) {
            try {
                if (foodService.expireReservation(post.getId())) {
                    released++;
                }
            } catch (RuntimeException e) {
                // One bad row must not abandon the rest of the batch; the next
                // sweep will retry it.
                log.warn("Could not release reservation on food post {}", post.getId(), e);
            }
        }
        return released;
    }
}
