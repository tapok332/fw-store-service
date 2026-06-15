package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.CreateReviewCommand;
import kh.karazin.foodwise.store.application.port.in.StoreReviewUseCase;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.application.port.out.StoreReviewRepository;
import kh.karazin.foodwise.store.domain.ProfileId;
import kh.karazin.foodwise.store.domain.StoreId;
import kh.karazin.foodwise.store.domain.StoreReview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Store review creation. The acting profile id is supplied by the gateway via
 * the {@code X-User-Id} header, not via the request body.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class StoreReviewService implements StoreReviewUseCase {

    private final StoreReviewRepository reviewRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public StoreReview createReview(CreateReviewCommand command) {
        StoreId storeId = new StoreId(command.storeId());
        if (!storeRepository.existsById(storeId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + command.storeId());
        }

        StoreReview review = StoreReview.create(
                storeId,
                new ProfileId(command.profileId()),
                command.orderId(),
                command.rating(),
                command.comment());
        StoreReview saved = reviewRepository.save(review);
        log.info("Created review {} for store {} by profile {}",
                saved.id().value(), command.storeId(), command.profileId());
        return saved;
    }
}
