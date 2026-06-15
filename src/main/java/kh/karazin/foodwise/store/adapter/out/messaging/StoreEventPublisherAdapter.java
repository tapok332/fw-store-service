package kh.karazin.foodwise.store.adapter.out.messaging;

import kh.karazin.foodwise.common.event.EventTopics;
import kh.karazin.foodwise.common.outbox.OutboxPublisher;
import kh.karazin.foodwise.store.application.port.out.StoreEventPublisher;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Outbound adapter publishing store domain events via the transactional outbox.
 * Builds the exact event envelope (topic / key / type / payload) the prior
 * service emitted, preserving the wire contract for consumers.
 */
@Component
@RequiredArgsConstructor
class StoreEventPublisherAdapter implements StoreEventPublisher {

    private final OutboxPublisher outboxPublisher;

    @Override
    public void menuItemCreated(MenuItemId menuItemId, StoreId storeId) {
        outboxPublisher.saveEvent(
                EventTopics.MENU_ITEM_UPDATED,
                menuItemId.value().toString(),
                "menu-item.created",
                Map.of("menuItemId", menuItemId.value(), "storeId", storeId.value()),
                UUID.randomUUID());
    }

    @Override
    public void menuItemUpdated(MenuItemId menuItemId, StoreId storeId) {
        outboxPublisher.saveEvent(
                EventTopics.MENU_ITEM_UPDATED,
                menuItemId.value().toString(),
                "menu-item.updated",
                Map.of("menuItemId", menuItemId.value(), "storeId", storeId.value()),
                UUID.randomUUID());
    }
}
