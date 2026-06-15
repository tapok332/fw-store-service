package kh.karazin.foodwise.store.domain;

/**
 * Promotional banner shown for a store.
 */
public record Promo(
        PromoId id,
        StoreId storeId,
        String title,
        String description,
        String emoji,
        String bgColor,
        String accentColor,
        Boolean active,
        Integer priority
) {

    /**
     * Factory for a brand-new promo (no persistence id yet). {@code active}
     * defaults to {@code true} and {@code priority} to {@code 0} when not set,
     * preserving prior behavior.
     */
    public static Promo create(StoreId storeId,
                               String title,
                               String description,
                               String emoji,
                               String bgColor,
                               String accentColor,
                               Boolean active,
                               Integer priority) {
        return new Promo(null, storeId, title, description, emoji, bgColor, accentColor,
                active != null ? active : Boolean.TRUE,
                priority != null ? priority : 0);
    }
}
