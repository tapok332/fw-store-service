package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.common.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Command to create a combo for a store from a set of menu item ids. */
public record CreateComboCommand(
        UUID storeId,
        String title,
        Money price,
        String imageUrl,
        BigDecimal savings,
        List<UUID> menuItemIds
) {
}
