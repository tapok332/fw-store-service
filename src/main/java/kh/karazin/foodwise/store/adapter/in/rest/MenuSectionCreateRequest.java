package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new menu section.
 *
 * <p>Maps to the {@code menu_sections} table; only {@code title} and
 * {@code sortOrder} are persisted (the existing schema does not have
 * description/icon columns).
 */
public record MenuSectionCreateRequest(
        @NotBlank
        @Size(max = 100)
        String title,

        @NotNull
        @Min(0)
        Integer sortOrder
) {}
