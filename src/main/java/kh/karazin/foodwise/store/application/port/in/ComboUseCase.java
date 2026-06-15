package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.Combo;

/** Combo (menu-item bundle) creation. */
public interface ComboUseCase {

    Combo createCombo(CreateComboCommand command);
}
