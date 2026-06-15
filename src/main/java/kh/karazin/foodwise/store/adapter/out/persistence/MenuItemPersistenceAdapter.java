package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.MenuItemRepository;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter implementing the {@link MenuItemRepository} outbound port.
 */
@Component
@RequiredArgsConstructor
class MenuItemPersistenceAdapter implements MenuItemRepository {

    private final StoreMenuItemJpaRepository menuItemJpaRepository;
    private final StoreJpaRepository storeJpaRepository;

    @Override
    public Page<MenuItem> findByStoreId(StoreId storeId, Pageable pageable) {
        return menuItemJpaRepository.findByStoreId(storeId.value(), pageable)
                .map(PersistenceMappers::toMenuItem);
    }

    @Override
    public List<MenuItem> findAvailableByStoreId(StoreId storeId) {
        return menuItemJpaRepository.findByStoreIdAndAvailableTrue(storeId.value()).stream()
                .map(PersistenceMappers::toMenuItem)
                .toList();
    }

    @Override
    public List<MenuItem> searchByStoreIdAndQuery(StoreId storeId, String query) {
        return menuItemJpaRepository.searchByStoreIdAndQuery(storeId.value(), query).stream()
                .map(PersistenceMappers::toMenuItem)
                .toList();
    }

    @Override
    public Optional<MenuItem> findById(MenuItemId itemId) {
        return menuItemJpaRepository.findById(itemId.value()).map(PersistenceMappers::toMenuItem);
    }

    @Override
    public List<MenuItem> findAllById(Collection<MenuItemId> ids) {
        List<java.util.UUID> raw = ids.stream().map(MenuItemId::value).toList();
        return menuItemJpaRepository.findAllById(raw).stream()
                .map(PersistenceMappers::toMenuItem)
                .toList();
    }

    @Override
    public boolean existsById(MenuItemId itemId) {
        return menuItemJpaRepository.existsById(itemId.value());
    }

    @Override
    public MenuItem save(MenuItem menuItem) {
        StoreMenuItemEntity entity = menuItem.id() == null ? newEntity(menuItem) : reconcile(menuItem);
        return PersistenceMappers.toMenuItem(menuItemJpaRepository.save(entity));
    }

    private StoreMenuItemEntity newEntity(MenuItem menuItem) {
        // The existing create flow never links a section at creation time; the
        // inbound sectionId (if any) is intentionally not persisted here.
        return StoreMenuItemEntity.builder()
                .name(menuItem.name())
                .description(menuItem.description())
                .price(menuItem.price())
                .imageUrl(menuItem.imageUrl())
                .legacyCategory(menuItem.legacyCategory())
                .available(menuItem.available() != null ? menuItem.available() : Boolean.TRUE)
                .store(storeJpaRepository.getReferenceById(menuItem.storeId().value()))
                .build();
    }

    private StoreMenuItemEntity reconcile(MenuItem menuItem) {
        StoreMenuItemEntity entity = menuItemJpaRepository.findById(menuItem.id().value())
                .orElseThrow(() -> new IllegalStateException(
                        "Menu item disappeared during transaction: " + menuItem.id().value()));
        entity.setName(menuItem.name());
        entity.setDescription(menuItem.description());
        entity.setPrice(menuItem.price());
        entity.setImageUrl(menuItem.imageUrl());
        entity.setLegacyCategory(menuItem.legacyCategory());
        entity.setAvailable(menuItem.available());
        return entity;
    }

    @Override
    public void deleteById(MenuItemId itemId) {
        menuItemJpaRepository.deleteById(itemId.value());
    }
}
