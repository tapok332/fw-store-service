package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.common.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Combo data transfer object.
 */
public record ComboDto(
        UUID id,
        String title,
        Money price,
        String imageUrl,
        BigDecimal savings,
        List<MenuItemDto> menuItems
) {}
