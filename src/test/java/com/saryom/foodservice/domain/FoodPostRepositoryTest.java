package com.saryom.foodservice.domain;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev")
class FoodPostRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Autowired
    private FoodPostRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void browseReturnsOnlyAvailableUnexpiredPosts() {
        save("Fresh apples", FoodType.PRODUCE, FoodStatus.AVAILABLE, null);
        save("Old bread", FoodType.BAKERY, FoodStatus.AVAILABLE, NOW.minusSeconds(3600)); // expired
        FoodPost reserved = save("Soup", FoodType.PREPARED_MEAL, FoodStatus.AVAILABLE, null);
        reserved.reserve("bob", NOW);
        repository.save(reserved);

        var page = repository.browse(null, null, NOW, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(FoodPost::getTitle).containsExactly("Fresh apples");
    }

    @Test
    void browseFiltersByTypeAndTitle() {
        save("Fresh apples", FoodType.PRODUCE, FoodStatus.AVAILABLE, null);
        save("Fresh bagels", FoodType.BAKERY, FoodStatus.AVAILABLE, null);

        var byType = repository.browse(null, FoodType.BAKERY, NOW, PageRequest.of(0, 10));
        assertThat(byType.getContent()).extracting(FoodPost::getTitle).containsExactly("Fresh bagels");

        var byText = repository.browse("apple", null, NOW, PageRequest.of(0, 10));
        assertThat(byText.getContent()).extracting(FoodPost::getTitle).containsExactly("Fresh apples");
    }

    /**
     * Two takers racing for the last portion. Both requests load the post while
     * it is still AVAILABLE, so both pass {@link FoodPost#reserve}'s status
     * guard — the guard alone cannot separate them. The @Version column is what
     * makes the loser's write fail instead of silently overwriting the winner.
     */
    @Test
    void concurrentReservationsCannotBothWin() {
        FoodPost post = save("Last soup", FoodType.PREPARED_MEAL, FoodStatus.AVAILABLE, null);
        entityManager.flush();
        entityManager.clear();

        // Each request gets its own copy, both at version 0, both seeing AVAILABLE.
        FoodPost alicesCopy = repository.findById(post.getId()).orElseThrow();
        entityManager.detach(alicesCopy);
        FoodPost bobsCopy = repository.findById(post.getId()).orElseThrow();
        entityManager.detach(bobsCopy);

        alicesCopy.reserve("alice", NOW);
        repository.saveAndFlush(alicesCopy);
        entityManager.clear();

        bobsCopy.reserve("bob", NOW);
        assertThatThrownBy(() -> repository.saveAndFlush(bobsCopy))
                .isInstanceOf(OptimisticLockingFailureException.class);

        entityManager.clear();
        FoodPost persisted = repository.findById(post.getId()).orElseThrow();
        assertThat(persisted.getClaimedBy()).isEqualTo("alice");
        assertThat(persisted.getStatus()).isEqualTo(FoodStatus.RESERVED);
    }

    private FoodPost save(String title, FoodType type, FoodStatus status, Instant bestBefore) {
        FoodPost post = new FoodPost(UUID.randomUUID(), "giver", title, "desc", "1 portion",
                type, DietaryType.UNSPECIFIED, "Fremont, CA", null, null, bestBefore, null, NOW);
        if (status == FoodStatus.CANCELLED) {
            post.cancel(NOW);
        }
        return repository.save(post);
    }
}
