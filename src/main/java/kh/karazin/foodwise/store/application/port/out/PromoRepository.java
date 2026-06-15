package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.Promo;
import kh.karazin.foodwise.store.domain.PromoId;
import kh.karazin.foodwise.store.domain.StoreId;

import java.util.List;
import java.util.Optional;

/** Outbound port for promo persistence. */
public interface PromoRepository {

    List<Promo> findActiveByStoreIdOrderByPriorityDesc(StoreId storeId);

    List<Promo> findByStoreId(StoreId storeId);

    Optional<Promo> findById(PromoId promoId);

    boolean existsById(PromoId promoId);

    Promo save(Promo promo);

    void deleteById(PromoId promoId);
}
