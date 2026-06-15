package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for finding nearby stores.
 */
public record NearbyStoreRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        Double radius
) {
    public NearbyStoreRequest {
        if (radius == null) radius = 5000.0;
    }
}
