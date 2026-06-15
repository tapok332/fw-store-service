package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.Promo;
import kh.karazin.foodwise.store.domain.PromoId;
import kh.karazin.foodwise.store.domain.StoreId;

import java.util.List;

/** Store promotion listing and CRUD. */
public interface PromoUseCase {

    List<Promo> getActivePromosByStoreId(StoreId storeId);

    List<Promo> getAllPromosByStoreId(StoreId storeId);

    Promo createPromo(CreatePromoCommand command);

    Promo updatePromo(PromoId promoId, UpdatePromoCommand command);

    void deletePromo(PromoId promoId);
}
