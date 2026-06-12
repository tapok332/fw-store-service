package kh.karazin.foodwise.store.controller;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.dto.ReviewCreateRequest;
import kh.karazin.foodwise.store.dto.ReviewDto;
import kh.karazin.foodwise.store.service.StoreReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin/seed endpoint for creating store reviews. The acting profile id is
 * provided by the API gateway via the {@code X-User-Id} header after JWT
 * validation.
 */
@RestController
@RequestMapping("/stores/{storeId}/reviews")
@RequiredArgsConstructor
public class StoreReviewController {

    private final StoreReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReviewDto> createReview(
            @PathVariable UUID storeId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.success(reviewService.createReview(storeId, userId, request));
    }
}
