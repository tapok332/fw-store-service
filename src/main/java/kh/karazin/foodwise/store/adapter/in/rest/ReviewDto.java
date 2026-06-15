package kh.karazin.foodwise.store.adapter.in.rest;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Store review data transfer object for API responses.
 */
public record ReviewDto(
        UUID id,
        UUID storeId,
        UUID profileId,
        UUID orderId,
        Short rating,
        String comment,
        LocalDateTime createdAt
) {}
