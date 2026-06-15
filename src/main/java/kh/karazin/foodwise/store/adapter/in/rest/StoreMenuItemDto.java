package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.common.validation.SafeUrl;

import java.util.UUID;

public record StoreMenuItemDto(
        UUID id,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @NotNull @Valid Money price,
        @Size(max = 500) @SafeUrl String imageUrl,
        @Size(max = 100) String legacyCategory,
        Boolean available,
        UUID storeId,
        UUID sectionId
) {}
