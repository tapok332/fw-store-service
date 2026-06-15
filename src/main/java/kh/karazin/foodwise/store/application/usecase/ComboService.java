package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.ComboUseCase;
import kh.karazin.foodwise.store.application.port.in.CreateComboCommand;
import kh.karazin.foodwise.store.application.port.out.ComboRepository;
import kh.karazin.foodwise.store.application.port.out.MenuItemRepository;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.domain.Combo;
import kh.karazin.foodwise.store.domain.ComboCompositionException;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Combo creation. Each referenced menu item must exist <em>and</em> belong to
 * the same store; otherwise the request is rejected with HTTP 400.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class ComboService implements ComboUseCase {

    private final ComboRepository comboRepository;
    private final StoreRepository storeRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    @Transactional
    public Combo createCombo(CreateComboCommand command) {
        StoreId storeId = new StoreId(command.storeId());
        if (!storeRepository.existsById(storeId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + command.storeId());
        }

        List<MenuItemId> ids = command.menuItemIds().stream().map(MenuItemId::new).toList();
        List<MenuItem> resolved = menuItemRepository.findAllById(ids);
        if (resolved.size() != command.menuItemIds().size()) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.INVALID_REQUEST,
                    "One or more menu items not found for combo creation");
        }

        Combo combo;
        try {
            combo = Combo.create(storeId, command.title(), command.price(),
                    command.imageUrl(), command.savings(), resolved);
        } catch (ComboCompositionException e) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.INVALID_REQUEST, e.getMessage());
        }

        Combo saved = comboRepository.save(combo);
        log.info("Created combo {} ({}) for store {} with {} items",
                saved.id().value(), saved.title(), command.storeId(), resolved.size());
        return saved;
    }
}
