package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.dto.ComboCreateRequest;
import kh.karazin.foodwise.store.dto.ComboDto;
import kh.karazin.foodwise.store.entity.ComboEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreMenuItemEntity;
import kh.karazin.foodwise.store.mapper.StoreMapper;
import kh.karazin.foodwise.store.repository.ComboRepository;
import kh.karazin.foodwise.store.repository.StoreMenuItemRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing store combos (bundles of menu items).
 *
 * <p>Each menu item referenced in {@code menuItemIds} must exist <em>and</em>
 * belong to the same store; otherwise the request is rejected with HTTP 400.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepository;
    private final StoreRepository storeRepository;
    private final StoreMenuItemRepository menuItemRepository;
    private final StoreMapper storeMapper;

    /**
     * Persist a new combo and link it to the given menu items.
     *
     * @throws FoodWiseException 404 when the store is missing,
     *                           400 when any menu item is missing or owned by another store.
     */
    @Transactional
    public ComboDto createCombo(UUID storeId, ComboCreateRequest request) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));

        List<StoreMenuItemEntity> resolved = menuItemRepository.findAllById(request.menuItemIds());
        if (resolved.size() != request.menuItemIds().size()) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.INVALID_REQUEST,
                    "One or more menu items not found for combo creation");
        }

        boolean foreignItem = resolved.stream()
                .anyMatch(item -> item.getStore() == null
                        || !storeId.equals(item.getStore().getId()));
        if (foreignItem) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.INVALID_REQUEST,
                    "Combo menu items must all belong to store " + storeId);
        }

        ComboEntity entity = ComboEntity.builder()
                .store(store)
                .title(request.title())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .savings(request.savings())
                .menuItems(new ArrayList<>(resolved))
                .build();

        ComboEntity saved = comboRepository.save(entity);
        log.info("Created combo {} ({}) for store {} with {} items",
                saved.getId(), saved.getTitle(), storeId, resolved.size());
        return storeMapper.toComboDto(saved);
    }
}
