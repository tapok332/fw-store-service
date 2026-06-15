package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.common.money.Money;

/**
 * Command to update an existing menu item. {@code available} is optional — a
 * {@code null} value leaves the current availability unchanged.
 */
public record UpdateMenuItemCommand(
        String name,
        String description,
        Money price,
        String imageUrl,
        String legacyCategory,
        Boolean available
) {
}
