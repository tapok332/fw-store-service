package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.StoreReview;

/** Outbound port for store review persistence. */
public interface StoreReviewRepository {

    StoreReview save(StoreReview review);
}
