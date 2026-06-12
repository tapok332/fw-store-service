package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.dto.internal.InternalMenuItemDto;
import kh.karazin.foodwise.common.dto.internal.InternalSurpriseBoxDto;
import kh.karazin.foodwise.common.event.EventTopics;
import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.common.outbox.OutboxPublisher;
import kh.karazin.foodwise.store.client.SurpriseBoxServiceClient;
import kh.karazin.foodwise.store.dto.StoreMenuItemDto;
import kh.karazin.foodwise.store.entity.MenuSectionEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreMenuItemEntity;
import kh.karazin.foodwise.store.mapper.StoreMapper;
import kh.karazin.foodwise.store.repository.StoreMenuItemRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service for CRUD operations on store menu items.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreMenuItemService {

    private final StoreMenuItemRepository menuItemRepository;
    private final SurpriseBoxServiceClient surpriseBoxServiceClient;
    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final OutboxPublisher outboxPublisher;

    /**
     * Get paginated menu items for a store.
     */
    @Transactional(readOnly = true)
    public Page<StoreMenuItemDto> getMenuItems(UUID storeId, int page, int size) {
        return menuItemRepository.findByStoreId(storeId, PageRequest.of(page, size))
                .map(storeMapper::toStoreMenuItemDto);
    }

    /**
     * Get a single menu item by ID.
     */
    @Transactional(readOnly = true)
    public StoreMenuItemDto getMenuItem(UUID itemId) {
        StoreMenuItemEntity entity = menuItemRepository.findById(itemId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId));
        return storeMapper.toStoreMenuItemDto(entity);
    }

    /**
     * Internal-lane lookup for {@code GET /internal/menu-items/{id}} per ADR 0010.
     * Returns the typed wire DTO without {@code description}, {@code legacyCategory},
     * or {@code sectionId} — fields not consumed by cart-service.
     */
    @Transactional(readOnly = true)
    public InternalMenuItemDto getMenuItemForInternal(UUID itemId) {
        StoreMenuItemEntity entity = menuItemRepository.findById(itemId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId));
        return storeMapper.toInternalMenuItemDto(entity);
    }

    /**
     * Unified internal lookup: resolves {@code id} against {@code menu_items} first,
     * then delegates to surprisebox-service if not found.
     * Used by {@code GET /internal/items/{itemId}}.
     *
     * <p>For surprise boxes: {@code title} maps to {@code name},
     * {@code stock > 0} maps to {@code available}. Delegates to surprisebox-service
     * internal API to respect service data ownership (surprise_boxes live in the
     * surprisebox-service schema, not the store schema).
     */
    @Transactional(readOnly = true)
    public InternalMenuItemDto findItemById(UUID itemId) {
        // 1. try menu_items (own DB)
        var menuItem = menuItemRepository.findById(itemId);
        if (menuItem.isPresent()) {
            return storeMapper.toInternalMenuItemDto(menuItem.get());
        }

        // 2. delegate to surprisebox-service (different DB schema / service boundary)
        InternalSurpriseBoxDto box = surpriseBoxServiceClient.findById(itemId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND,
                        "Item not found (neither menu-item nor surprise-box): " + itemId));

        return new InternalMenuItemDto(
                box.id(),
                box.title(),
                box.price(),
                box.imageUrl(),
                box.store() != null ? box.store().storeId() : null,
                box.stock() != null && box.stock() > 0
        );
    }

    /**
     * Create a new menu item.
     */
    @Transactional
    public StoreMenuItemDto createMenuItem(StoreMenuItemDto dto) {
        StoreEntity store = storeRepository.findById(dto.storeId())
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + dto.storeId()));

        StoreMenuItemEntity entity = StoreMenuItemEntity.builder()
                .name(dto.name())
                .description(dto.description())
                .price(dto.price())
                .imageUrl(dto.imageUrl())
                .legacyCategory(dto.legacyCategory())
                .available(dto.available() != null ? dto.available() : true)
                .store(store)
                .build();

        StoreMenuItemEntity saved = menuItemRepository.save(entity);
        log.info("Created menu item {} for store {}", saved.getId(), dto.storeId());

        outboxPublisher.saveEvent(
                EventTopics.MENU_ITEM_UPDATED,
                saved.getId().toString(),
                "menu-item.created",
                Map.of("menuItemId", saved.getId(), "storeId", dto.storeId()),
                UUID.randomUUID()
        );

        return storeMapper.toStoreMenuItemDto(saved);
    }

    /**
     * Update an existing menu item.
     */
    @Transactional
    public StoreMenuItemDto updateMenuItem(UUID itemId, StoreMenuItemDto dto) {
        StoreMenuItemEntity entity = menuItemRepository.findById(itemId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId));

        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
        entity.setImageUrl(dto.imageUrl());
        entity.setLegacyCategory(dto.legacyCategory());
        if (dto.available() != null) entity.setAvailable(dto.available());

        StoreMenuItemEntity saved = menuItemRepository.save(entity);
        log.info("Updated menu item {}", itemId);

        outboxPublisher.saveEvent(
                EventTopics.MENU_ITEM_UPDATED,
                saved.getId().toString(),
                "menu-item.updated",
                Map.of("menuItemId", saved.getId(), "storeId", entity.getStore().getId()),
                UUID.randomUUID()
        );

        return storeMapper.toStoreMenuItemDto(saved);
    }

    /**
     * Delete a menu item.
     */
    @Transactional
    public void deleteMenuItem(UUID itemId) {
        if (!menuItemRepository.existsById(itemId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId);
        }
        menuItemRepository.deleteById(itemId);
        log.info("Deleted menu item {}", itemId);
    }
}
