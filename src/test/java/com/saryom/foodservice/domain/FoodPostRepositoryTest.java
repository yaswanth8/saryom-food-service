package com.saryom.foodservice.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
class FoodPostRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Autowired
    private FoodPostRepository repository;

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

    private FoodPost save(String title, FoodType type, FoodStatus status, Instant bestBefore) {
        FoodPost post = new FoodPost(UUID.randomUUID(), "giver", title, "desc", "1 portion",
                type, DietaryType.UNSPECIFIED, "Fremont, CA", null, null, bestBefore, null, NOW);
        if (status == FoodStatus.CANCELLED) {
            post.cancel(NOW);
        }
        return repository.save(post);
    }
}
