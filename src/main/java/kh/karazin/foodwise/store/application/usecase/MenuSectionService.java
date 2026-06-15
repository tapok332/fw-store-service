package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.MenuSectionUseCase;
import kh.karazin.foodwise.store.application.port.out.MenuSectionRepository;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.domain.MenuSection;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-store menu section creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class MenuSectionService implements MenuSectionUseCase {

    private final MenuSectionRepository sectionRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public MenuSection createSection(StoreId storeId, String title, Integer sortOrder) {
        if (!storeRepository.existsById(storeId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId.value());
        }

        MenuSection section = MenuSection.create(storeId, title, sortOrder);
        MenuSection saved = sectionRepository.save(section);
        log.info("Created menu section {} ({}) for store {}",
                saved.id().value(), saved.title(), storeId.value());
        return saved;
    }
}
