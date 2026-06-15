package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Search parameters for filtering stores.
 *
 * <p>{@code type} and {@code group} are mutually exclusive — the compact
 * constructor throws on both. {@code group} resolves to
 * {@code WHERE type IN (StoreType.typesIn(group))} in the persistence layer.
 */
public record StoreSearchParams(
        String search,
        StoreType type,
        StoreGroup group,
        UUID categoryId,
        String categorySlug,
        Double latitude,
        Double longitude,
        BigDecimal minRating,
        Double maxDistanceKm,
        Boolean openNow,
        List<Integer> priceLevels,
        SortField sort,
        int page,
        int size
) {

    public enum SortField {
        DISTANCE, RATING, PRICE_ASC, PRICE_DESC, RELEVANCE;

        public static SortField parse(String value) {
            if (value == null || value.isBlank()) return RELEVANCE;
            String key = value.contains(",") ? value.substring(0, value.indexOf(',')).trim() : value.trim();
            return switch (key) {
                case "distance" -> DISTANCE;
                case "rating" -> RATING;
                case "priceAsc" -> PRICE_ASC;
                case "priceDesc" -> PRICE_DESC;
                case "relevance" -> RELEVANCE;
                default -> throw new IllegalArgumentException("Unknown sort: " + value);
            };
        }
    }

    public StoreSearchParams {
        if (type != null && group != null) {
            throw new IllegalArgumentException("type and group are mutually exclusive");
        }
        if (priceLevels == null) priceLevels = List.of();
        if (openNow == null) openNow = false;
        if (sort == null) sort = SortField.RELEVANCE;
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
}
