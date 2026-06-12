package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.dto.PromoAdminDto;
import kh.karazin.foodwise.store.dto.PromoDto;
import kh.karazin.foodwise.store.entity.PromoEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.mapper.PromoMapper;
import kh.karazin.foodwise.store.repository.PromoRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing store promotions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromoService {

    private final PromoRepository promoRepository;
    private final StoreRepository storeRepository;
    private final PromoMapper promoMapper;

    /**
     * Get active promos for a store.
     */
    @Transactional(readOnly = true)
    public List<PromoDto> getActivePromosByStoreId(UUID storeId) {
        return promoRepository.findByStoreIdAndActiveTrueOrderByPriorityDesc(storeId)
                .stream()
                .map(promoMapper::toDto)
                .toList();
    }

    /**
     * Get all promos for a store (admin).
     */
    @Transactional(readOnly = true)
    public List<PromoAdminDto> getAllPromosByStoreId(UUID storeId) {
        return promoRepository.findByStoreId(storeId)
                .stream()
                .map(promoMapper::toAdminDto)
                .toList();
    }

    /**
     * Create a new promo.
     */
    @Transactional
    public PromoAdminDto createPromo(PromoAdminDto dto) {
        StoreEntity store = storeRepository.findById(dto.storeId())
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + dto.storeId()));

        PromoEntity entity = PromoEntity.builder()
                .store(store)
                .title(dto.title())
                .description(dto.description())
                .emoji(dto.emoji())
                .bgColor(dto.bgColor())
                .accentColor(dto.accentColor())
                .active(dto.active() != null ? dto.active() : true)
                .priority(dto.priority() != null ? dto.priority() : 0)
                .build();

        PromoEntity saved = promoRepository.save(entity);
        log.info("Created promo {} for store {}", saved.getId(), dto.storeId());
        return promoMapper.toAdminDto(saved);
    }

    /**
     * Update an existing promo.
     */
    @Transactional
    public PromoAdminDto updatePromo(UUID promoId, PromoAdminDto dto) {
        PromoEntity entity = promoRepository.findById(promoId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Promo not found: " + promoId));

        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setEmoji(dto.emoji());
        entity.setBgColor(dto.bgColor());
        entity.setAccentColor(dto.accentColor());
        if (dto.active() != null) entity.setActive(dto.active());
        if (dto.priority() != null) entity.setPriority(dto.priority());

        PromoEntity saved = promoRepository.save(entity);
        log.info("Updated promo {}", promoId);
        return promoMapper.toAdminDto(saved);
    }

    /**
     * Delete a promo.
     */
    @Transactional
    public void deletePromo(UUID promoId) {
        if (!promoRepository.existsById(promoId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Promo not found: " + promoId);
        }
        promoRepository.deleteById(promoId);
        log.info("Deleted promo {}", promoId);
    }
}
