package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.MenuSection;

/** Outbound port for menu section persistence. */
public interface MenuSectionRepository {

    MenuSection save(MenuSection section);
}
