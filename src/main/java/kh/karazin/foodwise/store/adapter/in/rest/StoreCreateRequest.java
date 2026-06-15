package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.common.validation.SafeUrl;
import kh.karazin.foodwise.store.domain.StoreType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request body for creating a new store. Used by admin/seed endpoints.
 *
 * <p>{@code lat} and {@code lng} are validated against WGS-84 ranges and
 * combined into a PostGIS geography Point (SRID 4326) by the persistence layer.
 */
public record StoreCreateRequest(
        @NotBlank
        String name,

        String description,

        @SafeUrl
        String imageUrl,

        @SafeUrl
        String heroImageUrl,

        StoreType type,

        UUID categoryId,

        @NotBlank
        String address,

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        BigDecimal lat,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        BigDecimal lng,

        @DecimalMin("0.0")
        @DecimalMax("5.0")
        BigDecimal rating,

        LocalTime opensAt,

        LocalTime closesAt,

        String phone,

        @SafeUrl
        String website,

        @Valid
        Money deliveryFee,

        @Valid
        Money minOrderAmount,

        @Min(1)
        @Max(4)
        Integer priceLevel
) {}
