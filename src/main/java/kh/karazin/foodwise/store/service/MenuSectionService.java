package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.dto.MenuSectionCreateRequest;
import kh.karazin.foodwise.store.dto.MenuSectionDto;
import kh.karazin.foodwise.store.entity.MenuSectionEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.repository.MenuSectionRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing per-store menu sections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuSectionService {

    private final MenuSectionRepository sectionRepository;
    private final StoreRepository storeRepository;

    /**
     * Create a new menu section for the given store.
     *
     * @throws FoodWiseException with HTTP 404 if the store does not exist.
     */
    @Transactional
    public MenuSectionDto createSection(UUID storeId, MenuSectionCreateRequest request) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));

        MenuSectionEntity entity = MenuSectionEntity.builder()
                .store(store)
                .title(request.title())
                .sortOrder(request.sortOrder())
                .build();

        MenuSectionEntity saved = sectionRepository.save(entity);
        log.info("Created menu section {} ({}) for store {}", saved.getId(), saved.getTitle(), storeId);
        return toDto(saved, storeId);
    }

    private MenuSectionDto toDto(MenuSectionEntity entity, UUID storeId) {
        return new MenuSectionDto(
                entity.getId(),
                storeId,
                entity.getTitle(),
                entity.getSortOrder()
        );
    }
}
