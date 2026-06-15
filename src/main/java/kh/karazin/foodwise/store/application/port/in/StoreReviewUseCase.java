package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.StoreReview;

/** Store review creation. */
public interface StoreReviewUseCase {

    StoreReview createReview(CreateReviewCommand command);
}
