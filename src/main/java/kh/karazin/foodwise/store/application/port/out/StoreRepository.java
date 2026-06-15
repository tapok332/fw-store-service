package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.application.port.in.StoreSearchParams;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreId;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/** Outbound port for store persistence and spatial queries. */
public interface StoreRepository {

    Optional<Store> findById(StoreId storeId);

    Page<Store> search(StoreSearchParams params);

    List<Store> findFeatured();

    List<Store> findNearby(double lat, double lon, double radius);

    Optional<Double> findDistanceMeters(StoreId storeId, double lat, double lon);

    boolean existsById(StoreId storeId);

    Store save(Store store);
}
