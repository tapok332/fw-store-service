package kh.karazin.foodwise.store.domain;

/**
 * Grouping of menu items within a store (e.g. "Breakfast", "Drinks").
 */
public record MenuSection(
        MenuSectionId id,
        StoreId storeId,
        String title,
        Integer sortOrder
) {

    /** Factory for a brand-new menu section (no persistence id yet). */
    public static MenuSection create(StoreId storeId, String title, Integer sortOrder) {
        return new MenuSection(null, storeId, title, sortOrder);
    }
}
