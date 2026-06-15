package kh.karazin.foodwise.store.domain;

import java.util.List;

/**
 * Composite read result for the store-detail screen: the store together with
 * its available menu items, combos, and an optional distance (in meters) from
 * a query point.
 */
public record StoreWithMenu(
        Store store,
        List<MenuItem> menuItems,
        List<Combo> combos,
        Double distance
) {
}
