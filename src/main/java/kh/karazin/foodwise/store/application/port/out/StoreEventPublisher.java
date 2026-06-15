package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.StoreId;

/** Outbound port for publishing store domain events (via the transactional outbox). */
public interface StoreEventPublisher {

    void menuItemCreated(MenuItemId menuItemId, StoreId storeId);

    void menuItemUpdated(MenuItemId menuItemId, StoreId storeId);
}
