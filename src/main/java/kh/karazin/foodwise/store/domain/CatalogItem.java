package kh.karazin.foodwise.store.domain;

import kh.karazin.foodwise.common.money.Money;

import java.util.UUID;

/**
 * Unified, minimal catalog item used by the internal item-lookup lane. Resolves
 * a menu item from this service or a surprise box from surprisebox-service to a
 * single shape consumed by cart-service. {@code id} is a raw {@link UUID}
 * because the lookup spans two distinct id spaces (menu items and boxes).
 */
public record CatalogItem(
        UUID id,
        String name,
        Money price,
        String imageUrl,
        UUID storeId,
        Boolean available
) {

    /** Projects a {@link MenuItem} onto the unified catalog-item shape. */
    public static CatalogItem of(MenuItem item) {
        return new CatalogItem(
                item.id() != null ? item.id().value() : null,
                item.name(),
                item.price(),
                item.imageUrl(),
                item.storeId() != null ? item.storeId().value() : null,
                item.available());
    }
}
