package kh.karazin.foodwise.store.dto;

import java.util.List;

/**
 * Response for menu search queries.
 */
public record MenuSearchResponse(
        String query,
        int totalResults,
        List<StoreMenuItemDto> items
) {}
