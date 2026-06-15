package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.CacheNames;
import kh.karazin.foodwise.store.application.port.in.CreateStoreCommand;
import kh.karazin.foodwise.store.application.port.in.StoreSearchParams;
import kh.karazin.foodwise.store.application.port.in.StoreUseCase;
import kh.karazin.foodwise.store.application.port.out.CategoryRepository;
import kh.karazin.foodwise.store.application.port.out.ComboRepository;
import kh.karazin.foodwise.store.application.port.out.MenuItemRepository;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.CategoryId;
import kh.karazin.foodwise.store.domain.Combo;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuSearchResult;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreId;
import kh.karazin.foodwise.store.domain.StoreWithMenu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Store operations: search, retrieval, menu queries, and creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StoreService implements StoreUseCase {

    private final StoreRepository storeRepository;
    private final MenuItemRepository menuItemRepository;
    private final ComboRepository comboRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<Store> searchStores(StoreSearchParams params) {
        return storeRepository.search(params);
    }

    @Override
    public StoreWithMenu getStoreById(StoreId storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId.value()));

        List<MenuItem> menuItems = menuItemRepository.findAvailableByStoreId(storeId);
        List<Combo> combos = comboRepository.findByStoreId(storeId);

        return new StoreWithMenu(store, menuItems, combos, null);
    }

    @Override
    public StoreWithMenu getStoreByIdWithDistance(StoreId storeId, double lat, double lon) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId.value()));

        List<MenuItem> menuItems = menuItemRepository.findAvailableByStoreId(storeId);
        List<Combo> combos = comboRepository.findByStoreId(storeId);

        Double distance = storeRepository.findDistanceMeters(storeId, lat, lon).orElse(null);
        return new StoreWithMenu(store, menuItems, combos, distance);
    }

    @Override
    public Store getStoreForInternal(StoreId storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId.value()));
    }

    @Override
    public List<Store> getFeaturedStores() {
        return storeRepository.findFeatured();
    }

    @Override
    public List<Store> findNearbyStores(double lat, double lon, double radius) {
        return storeRepository.findNearby(lat, lon, radius);
    }

    @Override
    public MenuSearchResult searchMenu(StoreId storeId, String query) {
        if (!storeRepository.existsById(storeId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId.value());
        }
        List<MenuItem> items = menuItemRepository.searchByStoreIdAndQuery(storeId, query);
        return new MenuSearchResult(query, items);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.STORES, allEntries = true)
    public Store createStore(CreateStoreCommand command) {
        Category category = null;
        if (command.categoryId() != null) {
            category = categoryRepository.findById(new CategoryId(command.categoryId()))
                    .orElseThrow(() -> FoodWiseException.errorWithDescription(
                            FoodWiseErrorCode.ENTITY_NOT_FOUND,
                            "Category not found: " + command.categoryId()));
        }

        Store store = Store.create(
                command.name(),
                command.description(),
                command.imageUrl(),
                command.heroImageUrl(),
                command.type(),
                category,
                command.address(),
                command.lat() != null ? command.lat().doubleValue() : null,
                command.lng() != null ? command.lng().doubleValue() : null,
                command.rating(),
                command.opensAt(),
                command.closesAt(),
                command.phone(),
                command.website(),
                command.deliveryFee(),
                command.minOrderAmount(),
                command.priceLevel());

        Store saved = storeRepository.save(store);
        log.info("Created store {} ({})", saved.id().value(), saved.name());
        return saved;
    }
}
