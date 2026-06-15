package kh.karazin.foodwise.store.adapter.in.rest;

import java.util.UUID;

/**
 * Public-facing promo DTO.
 */
public record PromoDto(
        UUID id,
        String title,
        String description,
        String emoji,
        String bgColor,
        String accentColor,
        Integer priority
) {}
