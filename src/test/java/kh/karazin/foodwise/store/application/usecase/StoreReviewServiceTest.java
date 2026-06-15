package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.CreateReviewCommand;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.application.port.out.StoreReviewRepository;
import kh.karazin.foodwise.store.domain.ProfileId;
import kh.karazin.foodwise.store.domain.ReviewId;
import kh.karazin.foodwise.store.domain.StoreId;
import kh.karazin.foodwise.store.domain.StoreReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
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

    private StoreReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new StoreReviewService(reviewRepository, storeRepository);
    }

    @Test
    void createReview_persistsReviewLinkedToStoreAndProfile_whenStoreExists() {
        UUID storeId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        var command = new CreateReviewCommand(storeId, profileId, null, (short) 5, "Great food!");

        when(storeRepository.existsById(new StoreId(storeId))).thenReturn(true);
        when(reviewRepository.save(any(StoreReview.class))).thenAnswer(inv -> {
            StoreReview r = inv.getArgument(0);
            return new StoreReview(new ReviewId(UUID.randomUUID()), r.storeId(), r.profileId(),
                    r.orderId(), r.rating(), r.comment(), LocalDateTime.now());
        });

        StoreReview result = reviewService.createReview(command);

        assertThat(result.id()).isNotNull();
        assertThat(result.storeId()).isEqualTo(new StoreId(storeId));
        assertThat(result.profileId()).isEqualTo(new ProfileId(profileId));
        assertThat(result.rating()).isEqualTo((short) 5);
        assertThat(result.comment()).isEqualTo("Great food!");

        ArgumentCaptor<StoreReview> captor = ArgumentCaptor.forClass(StoreReview.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().storeId()).isEqualTo(new StoreId(storeId));
        assertThat(captor.getValue().profileId()).isEqualTo(new ProfileId(profileId));
    }

    @Test
    void createReview_throwsEntityNotFound_whenStoreDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        var command = new CreateReviewCommand(storeId, UUID.randomUUID(), null, (short) 4, null);
        when(storeRepository.existsById(new StoreId(storeId))).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(command))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(reviewRepository, never()).save(any());
    }
}
