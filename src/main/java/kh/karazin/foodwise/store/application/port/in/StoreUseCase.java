package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.MenuSearchResult;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreId;
import kh.karazin.foodwise.store.domain.StoreWithMenu;
import org.springframework.data.domain.Page;

import java.util.List;

/** Store discovery and management operations. */
public interface StoreUseCase {

    Page<Store> searchStores(StoreSearchParams params);

    StoreWithMenu getStoreById(StoreId storeId);

    StoreWithMenu getStoreByIdWithDistance(StoreId storeId, double lat, double lon);

    Store getStoreForInternal(StoreId storeId);

    List<Store> getFeaturedStores();

    List<Store> findNearbyStores(double lat, double lon, double radius);

    MenuSearchResult searchMenu(StoreId storeId, String query);

    Store createStore(CreateStoreCommand command);
}
