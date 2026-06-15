package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.CatalogItem;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.StoreId;
import org.springframework.data.domain.Page;

import java.util.UUID;

/** Menu item CRUD plus the internal item-lookup lane. */
public interface MenuItemUseCase {

    Page<MenuItem> getMenuItems(StoreId storeId, int page, int size);

    MenuItem getMenuItem(MenuItemId itemId);

    CatalogItem getMenuItemForInternal(MenuItemId itemId);

    /** Resolves {@code itemId} against menu items first, then surprise boxes. */
    CatalogItem findItemById(UUID itemId);

    MenuItem createMenuItem(MenuItem menuItem);

    MenuItem updateMenuItem(MenuItemId itemId, UpdateMenuItemCommand command);

    void deleteMenuItem(MenuItemId itemId);
}
