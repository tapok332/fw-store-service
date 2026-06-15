package kh.karazin.foodwise.store.domain;

import kh.karazin.foodwise.common.money.Money;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Read-model projection of a surprise box for the home screen, joined with its
 * owning store's name/address/location. Surprise boxes are owned by
 * surprisebox-service; store-service only reads them for discovery, so this is
 * an intentionally anemic read-model with a single derived metric.
 */
public record SurpriseBoxView(
        UUID id,
        UUID storeId,
        String title,
        String description,
        String imageUrl,
        Money price,
        Money retailPrice,
        Integer stock,
        LocalTime pickupFrom,
        LocalTime pickupTo,
        Boolean deliveryAvailable,
        String category,
        BigDecimal rating,
        String storeName,
        String storeAddress,
        Double storeLatitude,
        Double storeLongitude
) {

    /**
     * Discount percentage of {@code price} relative to {@code retailPrice},
     * rounded to the nearest integer and clamped to {@code [0, ...]}. Returns
     * {@code 0} when either price is absent or the retail price is non-positive.
     */
    public int discountPercentage() {
        if (price != null && retailPrice != null && retailPrice.amountMinor() > 0) {
            long retail = retailPrice.amountMinor();
            long current = price.amountMinor();
            return Math.max(0, (int) Math.round((retail - current) * 100.0 / retail));
        }
        return 0;
    }
}
