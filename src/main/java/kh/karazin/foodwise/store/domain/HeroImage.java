package kh.karazin.foodwise.store.domain;

/**
 * Homepage hero banner image.
 */
public record HeroImage(
        Integer id,
        String key,
        String url,
        String icon
) {

    /** Factory for a brand-new hero image (no persistence id yet). */
    public static HeroImage create(String key, String url, String icon) {
        return new HeroImage(null, key, url, icon);
    }
}
