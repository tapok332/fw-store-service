package kh.karazin.foodwise.store.domain;

import java.util.UUID;

/** Value object identifying the profile that authored a {@link StoreReview}. */
public record ProfileId(UUID value) {
}
