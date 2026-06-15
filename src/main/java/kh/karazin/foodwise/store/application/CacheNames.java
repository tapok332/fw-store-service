package kh.karazin.foodwise.store.application;

/**
 * Cache region names shared between the cache-annotated use cases and the
 * {@code CacheConfig} bean. Declared in the application layer so the use cases
 * never depend on the {@code config} package (enforced by ArchUnit).
 */
public final class CacheNames {

    public static final String CATEGORIES = "categories";
    public static final String STORES = "stores";
    public static final String HERO_IMAGES = "heroImages";
    public static final String CATEGORY_ICONS = "categoryIcons";

    private CacheNames() {
    }
}
