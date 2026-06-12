package kh.karazin.foodwise.store.dto;

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
