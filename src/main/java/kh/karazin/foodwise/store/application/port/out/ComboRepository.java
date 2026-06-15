package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.Combo;
import kh.karazin.foodwise.store.domain.StoreId;

import java.util.List;

/** Outbound port for combo persistence. */
public interface ComboRepository {

    List<Combo> findByStoreId(StoreId storeId);

    Combo save(Combo combo);
}
