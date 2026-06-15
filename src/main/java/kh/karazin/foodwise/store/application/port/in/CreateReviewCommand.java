package kh.karazin.foodwise.store.application.port.in;

import java.util.UUID;

/**
 * Command to create a store review. {@code profileId} is supplied by the
 * gateway via the {@code X-User-Id} header, not by the request body.
 */
public record CreateReviewCommand(
        UUID storeId,
        UUID profileId,
        UUID orderId,
        Short rating,
        String comment
) {
}
