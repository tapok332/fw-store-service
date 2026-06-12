package kh.karazin.foodwise.store.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for creating a new store review.
 *
 * <p>The {@code profileId} (review author) is provided via the {@code X-User-Id}
 * request header by the API gateway after JWT validation, not via the body.
 */
public record ReviewCreateRequest(
        @NotNull
        @Min(1)
        @Max(5)
        Short rating,

        @Size(max = 2000)
        String comment,

        UUID orderId
) {}
