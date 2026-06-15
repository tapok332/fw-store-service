package kh.karazin.foodwise.store.domain;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cuisine-style or department-style classification for stores
 * ("Asian", "Pizza", "Produce", "Dairy", ...).
 *
 * <p>{@code slug} is the URL-friendly stable identifier. {@code name} is the
 * canonical English code label; display names come from {@link #translations}
 * resolved by the request locale. {@code applicableTypes} drives the page
 * partition between {@link StoreGroup}s.
 */
public record Category(
        CategoryId id,
        String name,
        String slug,
        String iconName,
        Map<String, String> translations,
        Set<StoreType> applicableTypes
) {

    private static final String DEFAULT_LOCALE = "en";

    /** Factory for a brand-new category (no persistence id yet). */
    public static Category create(String name,
                                  String slug,
                                  String iconName,
                                  Map<String, String> translations,
                                  Set<StoreType> applicableTypes) {
        return new Category(null, name, slug, iconName, translations, applicableTypes);
    }

    /**
     * Resolves the display name for {@code locale}, falling back to the English
     * translation and finally to the canonical {@code name}.
     */
    public String resolveName(Locale locale) {
        String key = locale != null ? locale.getLanguage() : DEFAULT_LOCALE;
        String hit = translations.get(key);
        if (hit != null) return hit;
        String en = translations.get(DEFAULT_LOCALE);
        if (en != null) return en;
        return name;
    }

    /** Applicable {@link StoreType}s, sorted by natural enum order. */
    public List<StoreType> sortedTypes() {
        return applicableTypes.stream().sorted().toList();
    }

    /** {@link StoreGroup}s derived from {@link #applicableTypes}, deduplicated and sorted. */
    public List<StoreGroup> applicableGroups() {
        return applicableTypes.stream()
                .map(StoreType::group)
                .distinct()
                .sorted()
                .toList();
    }

    /** Whether this category applies to any of the given types. */
    public boolean appliesToAnyOf(Set<StoreType> types) {
        return applicableTypes.stream().anyMatch(types::contains);
    }

    /** {@code "Asian Fusion"} → {@code "asian-fusion"}. */
    public static String slugify(String value) {
        return value == null ? null : value
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
