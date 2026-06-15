package kh.karazin.foodwise.store.domain;

/**
 * Macro-grouping of {@link StoreType}s for the page-level partition between
 * "restaurants" ({@link #FOOD_SERVICE}: prepared food, dine-in/delivery) and
 * "stores" ({@link #RETAIL}: physical products).
 *
 * <p>Owned by {@link StoreType#group()} — never stored on its own column.
 */
public enum StoreGroup {
    FOOD_SERVICE,
    RETAIL
}
