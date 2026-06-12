package kh.karazin.foodwise.store.dto;

import java.util.UUID;

/**
 * Menu section data transfer object for API responses.
 */
public record MenuSectionDto(
        UUID id,
        UUID storeId,
        String title,
        Integer sortOrder
) {}
