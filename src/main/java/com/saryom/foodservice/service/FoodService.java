package com.saryom.foodservice.service;

import com.saryom.foodservice.domain.FoodPost;
import com.saryom.foodservice.domain.FoodPostRepository;
import com.saryom.foodservice.domain.FoodSort;
import com.saryom.foodservice.domain.FoodStatus;
import com.saryom.foodservice.domain.FoodType;
import com.saryom.foodservice.domain.Haversine;
import com.saryom.foodservice.error.NotFoundException;
import com.saryom.foodservice.events.DomainEventPublisher;
import com.saryom.foodservice.events.FoodClaimedEvent;
import com.saryom.foodservice.events.FoodPostedEvent;
import com.saryom.foodservice.events.FoodReservationExpiredEvent;
import com.saryom.foodservice.web.dto.CreateFoodRequest;
import com.saryom.foodservice.web.dto.FoodCardResponse;
import com.saryom.foodservice.web.dto.FoodDetailResponse;
import com.saryom.foodservice.web.dto.UpdateFoodRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Food-post lifecycle, browse/search/geo, and reservations. */
@Service
public class FoodService {

    private static final double DEFAULT_RADIUS_MILES = 50.0;

    private final FoodPostRepository posts;
    private final DomainEventPublisher events;
    private final Clock clock;

    public FoodService(FoodPostRepository posts, DomainEventPublisher events, Clock clock) {
        this.posts = posts;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Browse available food. When coordinates are supplied, results are filtered to
     * within {@code radiusMiles} and each card carries its distance; otherwise a
     * straight paged/sorted database query is used.
     */
    @Transactional(readOnly = true)
    public Page<FoodCardResponse> browse(String q, FoodType type, FoodSort sort,
                                         Double lat, Double lng, Double radiusMiles,
                                         int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        var now = clock.instant();
        String needle = blankToNull(q);

        if (lat != null && lng != null) {
            return browseByDistance(needle, type, sort, lat, lng,
                    radiusMiles == null ? DEFAULT_RADIUS_MILES : radiusMiles, page, pageSize, now);
        }

        Pageable pageable = PageRequest.of(page, pageSize, sort.toSort());
        return posts.browse(needle, type, now, pageable)
                .map(f -> FoodCardResponse.from(f, null));
    }

    private Page<FoodCardResponse> browseByDistance(String q, FoodType type, FoodSort sort,
                                                    double lat, double lng, double radiusMiles,
                                                    int page, int pageSize, java.time.Instant now) {
        List<FoodPost> candidates = posts.browseWithCoordinates(q, type, now);

        Map<UUID, Double> distances = candidates.stream().collect(Collectors.toMap(
                FoodPost::getId, f -> Haversine.miles(lat, lng, f.getLat(), f.getLng())));

        List<FoodPost> within = candidates.stream()
                .filter(f -> distances.get(f.getId()) <= radiusMiles)
                .sorted(comparator(sort, distances))
                .toList();

        int from = Math.min(page * pageSize, within.size());
        int to = Math.min(from + pageSize, within.size());
        List<FoodCardResponse> content = within.subList(from, to).stream()
                .map(f -> FoodCardResponse.from(f, round(distances.get(f.getId()))))
                .toList();

        return new PageImpl<>(content, PageRequest.of(page, pageSize), within.size());
    }

    private Comparator<FoodPost> comparator(FoodSort sort, Map<UUID, Double> distances) {
        return switch (sort) {
            case NEAREST -> Comparator.comparingDouble(f -> distances.get(f.getId()));
            case EXPIRING_SOON -> Comparator.comparing(FoodPost::getBestBefore,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case NEWEST -> Comparator.comparing(FoodPost::getCreatedAt).reversed();
        };
    }

    @Transactional(readOnly = true)
    public FoodDetailResponse getDetail(UUID id, String viewerId) {
        return FoodDetailResponse.from(load(id), viewerId);
    }

    @Transactional
    public FoodDetailResponse create(String giverId, CreateFoodRequest req) {
        FoodPost post = new FoodPost(UUID.randomUUID(), giverId, req.title(), req.description(),
                req.quantity(), req.foodType(), req.dietary(), req.locationText(), req.lat(), req.lng(),
                req.bestBefore(), req.imageUrl(), clock.instant());
        FoodPost saved = posts.save(post);
        events.publish("food.posted", FoodPostedEvent.of(
                saved.getId(), giverId, saved.getTitle(), saved.getFoodType().name(), saved.getLocationText()));
        return FoodDetailResponse.from(saved, giverId);
    }

    @Transactional
    public FoodDetailResponse update(UUID id, String uid, UpdateFoodRequest req) {
        FoodPost post = load(id);
        post.requireOwner(uid);
        post.update(req.title(), req.description(), req.quantity(), req.foodType(), req.dietary(),
                req.locationText(), req.bestBefore(), req.imageUrl(), clock.instant());
        return FoodDetailResponse.from(posts.save(post), uid);
    }

    /** A taker reserves the food; notifies the giver via {@code food.claimed}. */
    @Transactional
    public FoodDetailResponse reserve(UUID id, String takerId) {
        FoodPost post = load(id);
        post.reserve(takerId, clock.instant());
        FoodPost saved = posts.save(post);
        events.publish("food.claimed", FoodClaimedEvent.of(
                saved.getId(), saved.getGiverId(), takerId, saved.getTitle()));
        return FoodDetailResponse.from(saved, takerId);
    }

    /** Release a reservation. Allowed for the taker who reserved it or the giver. */
    @Transactional
    public FoodDetailResponse release(UUID id, String uid) {
        FoodPost post = load(id);
        if (!post.isOwnedBy(uid) && !post.isClaimedBy(uid)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the giver or the taker who reserved it can release this post");
        }
        post.release(clock.instant());
        return FoodDetailResponse.from(posts.save(post), uid);
    }

    /**
     * Releases a reservation that was never collected, returning the food to the
     * pool. Each call is its own transaction so one failure inside a sweep
     * cannot roll back the rest of the batch.
     *
     * @return true when this call performed the release. False means someone got
     *     there first — the taker collected it, either party released it, or a
     *     concurrent sweep on another instance won. All are fine, and none is an
     *     error worth failing the sweep over.
     */
    @Transactional
    public boolean expireReservation(UUID id) {
        FoodPost post = posts.findById(id).orElse(null);
        if (post == null || post.getStatus() != FoodStatus.RESERVED) {
            return false;
        }
        String previousClaimer = post.getClaimedBy();
        post.release(clock.instant());
        FoodPost saved = posts.save(post);
        events.publish("food.reservation_expired", FoodReservationExpiredEvent.of(
                saved.getId(), saved.getGiverId(), previousClaimer, saved.getTitle()));
        return true;
    }

    @Transactional
    public FoodDetailResponse complete(UUID id, String uid) {
        FoodPost post = load(id);
        post.requireOwner(uid);
        post.complete(clock.instant());
        return FoodDetailResponse.from(posts.save(post), uid);
    }

    @Transactional
    public void cancel(UUID id, String uid) {
        FoodPost post = load(id);
        post.requireOwner(uid);
        post.cancel(clock.instant());
        posts.save(post);
    }

    @Transactional(readOnly = true)
    public List<FoodDetailResponse> mine(String uid) {
        return posts.findByGiverIdOrderByCreatedAtDesc(uid).stream()
                .map(f -> FoodDetailResponse.from(f, uid))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FoodDetailResponse> reservedByMe(String uid) {
        return posts.findByClaimedByAndStatusOrderByClaimedAtDesc(uid, FoodStatus.RESERVED).stream()
                .map(f -> FoodDetailResponse.from(f, uid))
                .toList();
    }

    private FoodPost load(UUID id) {
        return posts.findById(id).orElseThrow(() -> new NotFoundException("No food post " + id));
    }

    private static Double round(double miles) {
        return Math.round(miles * 10.0) / 10.0;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
