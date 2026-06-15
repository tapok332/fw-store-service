package kh.karazin.foodwise.store.domain;

import kh.karazin.foodwise.common.money.Money;

import java.math.BigDecimal;
import java.util.List;

/**
 * A bundle of menu items sold together at a combined price.
 *
 * <p>Aggregate invariant: every referenced {@link MenuItem} must belong to the
 * combo's own store — enforced by {@link #create}.
 */
public record Combo(
        ComboId id,
        StoreId storeId,
        String title,
        Money price,
        String imageUrl,
        BigDecimal savings,
        List<MenuItem> items
) {

    /**
     * Factory for a brand-new combo (no persistence id yet). Validates that all
     * {@code items} belong to {@code storeId}.
     *
     * @throws ComboCompositionException if any item is owned by another store.
     */
    public static Combo create(StoreId storeId,
                               String title,
                               Money price,
                               String imageUrl,
                               BigDecimal savings,
                               List<MenuItem> items) {
        boolean foreignItem = items.stream()
                .anyMatch(item -> item.storeId() == null
                        || !storeId.equals(item.storeId()));
        if (foreignItem) {
            throw new ComboCompositionException(
                    "Combo menu items must all belong to store " + storeId.value());
        }
        return new Combo(null, storeId, title, price, imageUrl, savings, List.copyOf(items));
    }
}
