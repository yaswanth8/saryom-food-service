package com.saryom.foodservice.service;

import com.saryom.foodservice.domain.DietaryType;
import com.saryom.foodservice.domain.FoodPost;
import com.saryom.foodservice.domain.FoodPostRepository;
import com.saryom.foodservice.domain.FoodStatus;
import com.saryom.foodservice.domain.FoodType;
import com.saryom.foodservice.error.ConflictException;
import com.saryom.foodservice.events.DomainEventPublisher;
import com.saryom.foodservice.events.FoodClaimedEvent;
import com.saryom.foodservice.events.FoodPostedEvent;
import com.saryom.foodservice.web.dto.CreateFoodRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FoodServiceTest {

    private final FoodPostRepository posts = mock(FoodPostRepository.class);
    private final DomainEventPublisher events = mock(DomainEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-17T00:00:00Z"), ZoneOffset.UTC);
    private FoodService service;

    @BeforeEach
    void setUp() {
        service = new FoodService(posts, events, clock);
        when(posts.save(any(FoodPost.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateFoodRequest request() {
        return new CreateFoodRequest("Fresh bread", "Two loaves", "2 loaves",
                FoodType.BAKERY, DietaryType.VEGETARIAN, "Fremont, CA", null, null, null, null);
    }

    private FoodPost availablePost(String giver) {
        return new FoodPost(UUID.randomUUID(), giver, "Fresh bread", "Two loaves", "2 loaves",
                FoodType.BAKERY, DietaryType.VEGETARIAN, "Fremont, CA", null, null, null, null,
                clock.instant());
    }

    @Test
    void createEmitsFoodPostedAndStartsAvailable() {
        var response = service.create("alice", request());

        assertThat(response.status()).isEqualTo("AVAILABLE");
        assertThat(response.mine()).isTrue();
        verify(events).publish(eq("food.posted"), any(FoodPostedEvent.class));
    }

    @Test
    void reserveMarksReservedAndNotifiesGiver() {
        FoodPost post = availablePost("alice");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        var response = service.reserve(post.getId(), "bob");

        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.reservedByMe()).isTrue();
        verify(events).publish(eq("food.claimed"), any(FoodClaimedEvent.class));
    }

    @Test
    void giverCannotReserveOwnPost() {
        FoodPost post = availablePost("alice");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.reserve(post.getId(), "alice"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reserveOnAlreadyReservedConflicts() {
        FoodPost post = availablePost("alice");
        post.reserve("bob", clock.instant());
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.reserve(post.getId(), "carol"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void completeRequiresReservationThenClosesPost() {
        FoodPost post = availablePost("alice");
        post.reserve("bob", clock.instant());
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        var response = service.complete(post.getId(), "alice");

        assertThat(response.status()).isEqualTo(FoodStatus.COMPLETED.name());
    }

    @Test
    void releaseReturnsPostToAvailable() {
        FoodPost post = availablePost("alice");
        post.reserve("bob", clock.instant());
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        var response = service.release(post.getId(), "bob");

        assertThat(response.status()).isEqualTo("AVAILABLE");
    }
}
