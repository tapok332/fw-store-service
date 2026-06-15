package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.MenuItemUseCase;
import kh.karazin.foodwise.store.application.port.in.UpdateMenuItemCommand;
import kh.karazin.foodwise.store.application.port.out.MenuItemRepository;
import kh.karazin.foodwise.store.application.port.out.StoreEventPublisher;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.application.port.out.SurpriseBoxCatalogGateway;
import kh.karazin.foodwise.store.domain.CatalogItem;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Menu item CRUD plus the unified internal item-lookup lane.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class MenuItemService implements MenuItemUseCase {

    private final MenuItemRepository menuItemRepository;
    private final StoreRepository storeRepository;
    private final SurpriseBoxCatalogGateway surpriseBoxCatalogGateway;
    private final StoreEventPublisher storeEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<MenuItem> getMenuItems(StoreId storeId, int page, int size) {
        return menuItemRepository.findByStoreId(storeId, PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItem getMenuItem(MenuItemId itemId) {
        return menuItemRepository.findById(itemId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId.value()));
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogItem getMenuItemForInternal(MenuItemId itemId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId.value()));
        return CatalogItem.of(item);
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogItem findItemById(UUID itemId) {
        Optional<MenuItem> menuItem = menuItemRepository.findById(new MenuItemId(itemId));
        if (menuItem.isPresent()) {
            return CatalogItem.of(menuItem.get());
        }
        return surpriseBoxCatalogGateway.findBoxAsItem(itemId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND,
                        "Item not found (neither menu-item nor surprise-box): " + itemId));
    }

    @Override
    @Transactional
    public MenuItem createMenuItem(MenuItem menuItem) {
        if (!storeRepository.existsById(menuItem.storeId())) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + menuItem.storeId().value());
        }

        MenuItem saved = menuItemRepository.save(menuItem);
        log.info("Created menu item {} for store {}", saved.id().value(), menuItem.storeId().value());

        storeEventPublisher.menuItemCreated(saved.id(), menuItem.storeId());
        return saved;
    }

    @Override
    @Transactional
    public MenuItem updateMenuItem(MenuItemId itemId, UpdateMenuItemCommand command) {
        MenuItem existing = menuItemRepository.findById(itemId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId.value()));

        MenuItem updated = new MenuItem(
                existing.id(),
                command.name(),
                command.description(),
                command.price(),
                command.imageUrl(),
                command.legacyCategory(),
                command.available() != null ? command.available() : existing.available(),
                existing.storeId(),
                existing.sectionId());

        MenuItem saved = menuItemRepository.save(updated);
        log.info("Updated menu item {}", itemId.value());

        storeEventPublisher.menuItemUpdated(saved.id(), existing.storeId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteMenuItem(MenuItemId itemId) {
        if (!menuItemRepository.existsById(itemId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId.value());
        }
        menuItemRepository.deleteById(itemId);
        log.info("Deleted menu item {}", itemId.value());
    }
}
