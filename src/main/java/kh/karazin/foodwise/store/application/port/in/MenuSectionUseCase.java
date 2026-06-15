package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.MenuSection;
import kh.karazin.foodwise.store.domain.StoreId;

/** Per-store menu section creation. */
public interface MenuSectionUseCase {

    MenuSection createSection(StoreId storeId, String title, Integer sortOrder);
}
