package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.store.domain.StoreType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/** Command to create a new store (admin/seed). */
public record CreateStoreCommand(
        String name,
        String description,
        String imageUrl,
        String heroImageUrl,
        StoreType type,
        UUID categoryId,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        BigDecimal rating,
        LocalTime opensAt,
        LocalTime closesAt,
        String phone,
        String website,
        Money deliveryFee,
        Money minOrderAmount,
        Integer priceLevel
) {
}
