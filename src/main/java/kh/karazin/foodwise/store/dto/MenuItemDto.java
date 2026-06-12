package kh.karazin.foodwise.store.dto;

import kh.karazin.foodwise.common.money.Money;

import java.util.UUID;

/**
 * Nested menu item DTO used within StoreDto.
 */
public record MenuItemDto(
        UUID id,
        String name,
        String description,
        Money price,
        String imageUrl,
        Boolean available
) {}
