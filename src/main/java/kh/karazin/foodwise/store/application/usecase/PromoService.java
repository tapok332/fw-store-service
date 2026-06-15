package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.CreatePromoCommand;
import kh.karazin.foodwise.store.application.port.in.PromoUseCase;
import kh.karazin.foodwise.store.application.port.in.UpdatePromoCommand;
import kh.karazin.foodwise.store.application.port.out.PromoRepository;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.domain.Promo;
import kh.karazin.foodwise.store.domain.PromoId;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Store promotion listing and CRUD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class PromoService implements PromoUseCase {

    private final PromoRepository promoRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Promo> getActivePromosByStoreId(StoreId storeId) {
        return promoRepository.findActiveByStoreIdOrderByPriorityDesc(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promo> getAllPromosByStoreId(StoreId storeId) {
        return promoRepository.findByStoreId(storeId);
    }

    @Override
    @Transactional
    public Promo createPromo(CreatePromoCommand command) {
        StoreId storeId = new StoreId(command.storeId());
        if (!storeRepository.existsById(storeId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + command.storeId());
        }

        Promo promo = Promo.create(
                storeId,
                command.title(),
                command.description(),
                command.emoji(),
                command.bgColor(),
                command.accentColor(),
                command.active(),
                command.priority());
        Promo saved = promoRepository.save(promo);
        log.info("Created promo {} for store {}", saved.id().value(), command.storeId());
        return saved;
    }

    @Override
    @Transactional
    public Promo updatePromo(PromoId promoId, UpdatePromoCommand command) {
        Promo existing = promoRepository.findById(promoId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Promo not found: " + promoId.value()));

        Promo updated = new Promo(
                existing.id(),
                existing.storeId(),
                command.title(),
                command.description(),
                command.emoji(),
                command.bgColor(),
                command.accentColor(),
                command.active() != null ? command.active() : existing.active(),
                command.priority() != null ? command.priority() : existing.priority());

        Promo saved = promoRepository.save(updated);
        log.info("Updated promo {}", promoId.value());
        return saved;
    }

    @Override
    @Transactional
    public void deletePromo(PromoId promoId) {
        if (!promoRepository.existsById(promoId)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.ENTITY_NOT_FOUND, "Promo not found: " + promoId.value());
        }
        promoRepository.deleteById(promoId);
        log.info("Deleted promo {}", promoId.value());
    }
}
