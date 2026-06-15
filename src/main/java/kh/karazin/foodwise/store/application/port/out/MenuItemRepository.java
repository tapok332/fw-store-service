package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.StoreId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Outbound port for menu item persistence and queries. */
public interface MenuItemRepository {

    Page<MenuItem> findByStoreId(StoreId storeId, Pageable pageable);

    List<MenuItem> findAvailableByStoreId(StoreId storeId);

    List<MenuItem> searchByStoreIdAndQuery(StoreId storeId, String query);

    Optional<MenuItem> findById(MenuItemId itemId);

    List<MenuItem> findAllById(Collection<MenuItemId> ids);

    boolean existsById(MenuItemId itemId);

    MenuItem save(MenuItem menuItem);

    void deleteById(MenuItemId itemId);
}
