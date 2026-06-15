package kh.karazin.foodwise.store.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Macro-classification of a store, orthogonal to the (cuisine) {@link Category}.
 *
 * <p>Persisted as STRING (not ORDINAL) so adding a new value never breaks the DB.
 *
 * <p>Each value belongs to a {@link StoreGroup}, used by the page-level filter
 * on {@code GET /stores?group=} and {@code GET /home/categories?group=}.
 */
public enum StoreType {
    RESTAURANT(StoreGroup.FOOD_SERVICE),
    CAFE      (StoreGroup.FOOD_SERVICE),
    BAKERY    (StoreGroup.FOOD_SERVICE),
    GROCERY   (StoreGroup.RETAIL),
    SWEETS    (StoreGroup.RETAIL),
    OTHER     (StoreGroup.RETAIL);

    private final StoreGroup group;

    StoreType(StoreGroup group) {
        this.group = group;
    }

    public StoreGroup group() {
        return group;
    }

    /**
     * @return immutable set of {@link StoreType}s belonging to {@code group}.
     */
    public static Set<StoreType> typesIn(StoreGroup group) {
        return Arrays.stream(values())
                .filter(t -> t.group == group)
                .collect(Collectors.toUnmodifiableSet());
    }
}
