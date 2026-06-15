package kh.karazin.foodwise.store.domain;

/**
 * Icon hint for a category, keyed by a stable string.
 */
public record CategoryIcon(
        Integer id,
        String key,
        String iconName
) {

    /** Factory for a brand-new category icon (no persistence id yet). */
    public static CategoryIcon create(String key, String iconName) {
        return new CategoryIcon(null, key, iconName);
    }
}
