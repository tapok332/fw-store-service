package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.PromoRepository;
import kh.karazin.foodwise.store.domain.Promo;
import kh.karazin.foodwise.store.domain.PromoId;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter implementing the {@link PromoRepository} outbound port.
 */
@Component
@RequiredArgsConstructor
class PromoPersistenceAdapter implements PromoRepository {

    private final PromoJpaRepository promoJpaRepository;
    private final StoreJpaRepository storeJpaRepository;

    @Override
    public List<Promo> findActiveByStoreIdOrderByPriorityDesc(StoreId storeId) {
        return promoJpaRepository.findByStoreIdAndActiveTrueOrderByPriorityDesc(storeId.value()).stream()
                .map(PersistenceMappers::toPromo)
                .toList();
    }

    @Override
    public List<Promo> findByStoreId(StoreId storeId) {
        return promoJpaRepository.findByStoreId(storeId.value()).stream()
                .map(PersistenceMappers::toPromo)
                .toList();
    }

    @Override
    public Optional<Promo> findById(PromoId promoId) {
        return promoJpaRepository.findById(promoId.value()).map(PersistenceMappers::toPromo);
    }

    @Override
    public boolean existsById(PromoId promoId) {
        return promoJpaRepository.existsById(promoId.value());
    }

    @Override
    public Promo save(Promo promo) {
        PromoEntity entity = promo.id() == null ? newEntity(promo) : reconcile(promo);
        return PersistenceMappers.toPromo(promoJpaRepository.save(entity));
    }

    private PromoEntity newEntity(Promo promo) {
        return PromoEntity.builder()
                .store(storeJpaRepository.getReferenceById(promo.storeId().value()))
                .title(promo.title())
                .description(promo.description())
                .emoji(promo.emoji())
                .bgColor(promo.bgColor())
                .accentColor(promo.accentColor())
                .active(promo.active())
                .priority(promo.priority())
                .build();
    }

    private PromoEntity reconcile(Promo promo) {
        PromoEntity entity = promoJpaRepository.findById(promo.id().value())
                .orElseThrow(() -> new IllegalStateException(
                        "Promo disappeared during transaction: " + promo.id().value()));
        entity.setTitle(promo.title());
        entity.setDescription(promo.description());
        entity.setEmoji(promo.emoji());
        entity.setBgColor(promo.bgColor());
        entity.setAccentColor(promo.accentColor());
        entity.setActive(promo.active());
        entity.setPriority(promo.priority());
        return entity;
    }

    @Override
    public void deleteById(PromoId promoId) {
        promoJpaRepository.deleteById(promoId.value());
    }
}
