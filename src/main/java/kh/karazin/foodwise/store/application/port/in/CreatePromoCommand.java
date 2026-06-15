package kh.karazin.foodwise.store.application.port.in;

import java.util.UUID;

/** Command to create a promo for a store. */
public record CreatePromoCommand(
        UUID storeId,
        String title,
        String description,
        String emoji,
        String bgColor,
        String accentColor,
        Boolean active,
        Integer priority
) {
}
