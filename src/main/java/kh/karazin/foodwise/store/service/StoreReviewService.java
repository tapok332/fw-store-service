package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.dto.ReviewCreateRequest;
import kh.karazin.foodwise.store.dto.ReviewDto;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreReviewEntity;
import kh.karazin.foodwise.store.repository.StoreRepository;
import kh.karazin.foodwise.store.repository.StoreReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing store reviews. Reviews are created on behalf of an
 * authenticated profile (user id is provided via the {@code X-User-Id}
 * gateway header, not via the request body).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreReviewService {

    private final StoreReviewRepository reviewRepository;
    private final StoreRepository storeRepository;

    /**
     * Persist a review for the given store on behalf of {@code profileId}.
     *
     * @throws FoodWiseException with HTTP 404 if the store does not exist.
     */
    @Transactional
    public ReviewDto createReview(UUID storeId, UUID profileId, ReviewCreateRequest request) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));

        StoreReviewEntity entity = StoreReviewEntity.builder()
                .store(store)
                .profileId(profileId)
                .orderId(request.orderId())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        StoreReviewEntity saved = reviewRepository.save(entity);
        log.info("Created review {} for store {} by profile {}", saved.getId(), storeId, profileId);
        return toDto(saved, storeId);
    }

    private ReviewDto toDto(StoreReviewEntity entity, UUID storeId) {
        return new ReviewDto(
                entity.getId(),
                storeId,
                entity.getProfileId(),
                entity.getOrderId(),
                entity.getRating(),
                entity.getComment(),
                entity.getCreatedAt()
        );
    }
}
