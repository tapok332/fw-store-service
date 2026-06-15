package kh.karazin.foodwise.store.domain;

import kh.karazin.foodwise.common.money.Money;

/**
 * A single item on a store's menu.
 */
public record MenuItem(
        MenuItemId id,
        String name,
        String description,
        Money price,
        String imageUrl,
        String legacyCategory,
        Boolean available,
        StoreId storeId,
        MenuSectionId sectionId
) {

    /**
     * Factory for a brand-new menu item (no persistence id yet). Availability
     * defaults to {@code true} when not supplied, preserving prior behavior.
     */
    public static MenuItem create(String name,
                                  String description,
                                  Money price,
                                  String imageUrl,
                                  String legacyCategory,
                                  Boolean available,
                                  StoreId storeId,
                                  MenuSectionId sectionId) {
        return new MenuItem(null, name, description, price, imageUrl, legacyCategory,
                available != null ? available : Boolean.TRUE, storeId, sectionId);
    }
}
