package kh.karazin.foodwise.store.domain;

import kh.karazin.foodwise.common.money.Money;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Store aggregate root — a physical place offering food or retail products.
 *
 * <p>Pure-Java domain model: geographic position is held as plain
 * {@code latitude}/{@code longitude} doubles; the PostGIS {@code Point} mapping
 * lives entirely in the persistence adapter. {@code Money} from fw-common is the
 * sanctioned shared value object (see ADR 0014).
 */
public record Store(
        StoreId id,
        String name,
        String description,
        String imageUrl,
        String heroImageUrl,
        StoreType type,
        Category category,
        String address,
        Double latitude,
        Double longitude,
        BigDecimal rating,
        LocalTime opensAt,
        LocalTime closesAt,
        String phone,
        String website,
        Money deliveryFee,
        Money minOrderAmount,
        Integer priceLevel
) {

    /**
     * Factory for a brand-new store (no persistence id yet). The type defaults
     * to {@link StoreType#RESTAURANT} when not supplied, preserving the prior
     * service behavior.
     */
    public static Store create(String name,
                               String description,
                               String imageUrl,
                               String heroImageUrl,
                               StoreType type,
                               Category category,
                               String address,
                               Double latitude,
                               Double longitude,
                               BigDecimal rating,
                               LocalTime opensAt,
                               LocalTime closesAt,
                               String phone,
                               String website,
                               Money deliveryFee,
                               Money minOrderAmount,
                               Integer priceLevel) {
        return new Store(
                null, name, description, imageUrl, heroImageUrl,
                type != null ? type : StoreType.RESTAURANT,
                category, address, latitude, longitude, rating,
                opensAt, closesAt, phone, website,
                deliveryFee, minOrderAmount, priceLevel);
    }

    /**
     * Check if the store is currently open based on opens_at/closes_at.
     * Caller must pass the store's wall-clock zone (configured via foodwise.store.timezone /
     * STORE_TIMEZONE env var) — opensAt/closesAt are stored as wall-clock, not UTC, so the
     * container default zone (often UTC) would misalign the comparison.
     */
    public boolean isCurrentlyOpen(ZoneId zone) {
        if (opensAt == null || closesAt == null) {
            return false;
        }
        LocalTime now = LocalTime.now(zone);
        if (closesAt.isAfter(opensAt)) {
            return !now.isBefore(opensAt) && !now.isAfter(closesAt);
        }
        // Handles overnight hours (e.g., 22:00 - 02:00)
        return !now.isBefore(opensAt) || !now.isAfter(closesAt);
    }

    /** Returns a copy of this store with a new hero image URL. */
    public Store withHeroImage(String newHeroImageUrl) {
        return new Store(
                id, name, description, imageUrl, newHeroImageUrl, type, category,
                address, latitude, longitude, rating, opensAt, closesAt, phone,
                website, deliveryFee, minOrderAmount, priceLevel);
    }
}
