package kh.karazin.foodwise.store.domain;

import java.util.List;

/**
 * Result of searching a store's menu for a free-text query.
 */
public record MenuSearchResult(
        String query,
        List<MenuItem> items
) {
}
