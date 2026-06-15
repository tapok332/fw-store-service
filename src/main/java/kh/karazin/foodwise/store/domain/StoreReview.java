package kh.karazin.foodwise.store.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A rating-and-comment review left by a profile for a store.
 */
public record StoreReview(
        ReviewId id,
        StoreId storeId,
        ProfileId profileId,
        UUID orderId,
        Short rating,
        String comment,
        LocalDateTime createdAt
) {

    /** Factory for a brand-new review (no persistence id / timestamp yet). */
    public static StoreReview create(StoreId storeId,
                                     ProfileId profileId,
                                     UUID orderId,
                                     Short rating,
                                     String comment) {
        return new StoreReview(null, storeId, profileId, orderId, rating, comment, null);
    }
}
