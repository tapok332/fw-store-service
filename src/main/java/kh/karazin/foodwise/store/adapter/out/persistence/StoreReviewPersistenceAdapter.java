package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.StoreReviewRepository;
import kh.karazin.foodwise.store.domain.ReviewId;
import kh.karazin.foodwise.store.domain.StoreReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter implementing the {@link StoreReviewRepository} outbound port.
 */
@Component
@RequiredArgsConstructor
class StoreReviewPersistenceAdapter implements StoreReviewRepository {

    private final StoreReviewJpaRepository reviewJpaRepository;
    private final StoreJpaRepository storeJpaRepository;

    @Override
    public StoreReview save(StoreReview review) {
        StoreReviewEntity entity = StoreReviewEntity.builder()
                .store(storeJpaRepository.getReferenceById(review.storeId().value()))
                .profileId(review.profileId().value())
                .orderId(review.orderId())
                .rating(review.rating())
                .comment(review.comment())
                .build();
        StoreReviewEntity saved = reviewJpaRepository.save(entity);
        return new StoreReview(
                new ReviewId(saved.getId()),
                review.storeId(),
                review.profileId(),
                saved.getOrderId(),
                saved.getRating(),
                saved.getComment(),
                saved.getCreatedAt());
    }
}
