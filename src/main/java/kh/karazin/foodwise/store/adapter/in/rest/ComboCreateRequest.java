package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.common.validation.SafeUrl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request body for creating a new combo (bundle of menu items).
 *
 * <p>All {@code menuItemIds} must reference menu items that belong to the
 * target store; otherwise the request is rejected with HTTP 400.
 */
public record ComboCreateRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotNull
        @Valid
        Money price,

        @Size(max = 512)
        @SafeUrl
        String imageUrl,

        BigDecimal savings,

        @NotEmpty
        List<UUID> menuItemIds
) {}
