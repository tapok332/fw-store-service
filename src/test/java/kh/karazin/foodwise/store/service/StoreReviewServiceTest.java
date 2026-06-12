package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.dto.ReviewCreateRequest;
import kh.karazin.foodwise.store.dto.ReviewDto;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreReviewEntity;
import kh.karazin.foodwise.store.repository.StoreRepository;
import kh.karazin.foodwise.store.repository.StoreReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreReviewServiceTest {

    @Mock private StoreReviewRepository reviewRepository;
    @Mock private StoreRepository storeRepository;

    @InjectMocks
    private StoreReviewService reviewService;

    @Test
    void createReview_persistsReviewLinkedToStoreAndProfile_whenStoreExists() {
        UUID storeId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        StoreEntity store = StoreEntity.builder().id(storeId).name("Bakery").build();
        var request = new ReviewCreateRequest((short) 5, "Great food!", null);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(reviewRepository.save(any(StoreReviewEntity.class))).thenAnswer(inv -> {
            StoreReviewEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ReviewDto result = reviewService.createReview(storeId, profileId, request);

        assertThat(result).isNotNull();
        assertThat(result.storeId()).isEqualTo(storeId);
        assertThat(result.profileId()).isEqualTo(profileId);
        assertThat(result.rating()).isEqualTo((short) 5);
        assertThat(result.comment()).isEqualTo("Great food!");

        ArgumentCaptor<StoreReviewEntity> captor = ArgumentCaptor.forClass(StoreReviewEntity.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getStore().getId()).isEqualTo(storeId);
        assertThat(captor.getValue().getProfileId()).isEqualTo(profileId);
    }

    @Test
    void createReview_throwsEntityNotFound_whenStoreDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        var request = new ReviewCreateRequest((short) 4, null, null);
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(storeId, profileId, request))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(reviewRepository, never()).save(any());
    }
}
