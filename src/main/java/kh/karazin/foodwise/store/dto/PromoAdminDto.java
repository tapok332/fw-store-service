package kh.karazin.foodwise.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Admin DTO for creating/updating promos.
 */
public record PromoAdminDto(
        UUID id,
        @NotNull UUID storeId,
        @NotBlank String title,
        String description,
        String emoji,
        String bgColor,
        String accentColor,
        Boolean active,
        Integer priority
) {}
