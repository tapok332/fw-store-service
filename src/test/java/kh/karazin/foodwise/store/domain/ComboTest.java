package kh.karazin.foodwise.store.domain;

import kh.karazin.foodwise.common.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComboTest {

    private static final Currency UAH = Currency.getInstance("UAH");

    private MenuItem itemOfStore(StoreId storeId) {
        return new MenuItem(new MenuItemId(UUID.randomUUID()), "Item", null,
                Money.ofMajor(new BigDecimal("50"), UAH), null, null, true, storeId, null);
    }

    @Test
    void create_succeeds_when_all_items_belong_to_store() {
        StoreId storeId = new StoreId(UUID.randomUUID());
        Combo combo = Combo.create(storeId, "Combo",
                Money.ofMajor(new BigDecimal("90"), UAH), null, new BigDecimal("10"),
                List.of(itemOfStore(storeId), itemOfStore(storeId)));

        assertThat(combo.id()).isNull();
        assertThat(combo.items()).hasSize(2);
        assertThat(combo.storeId()).isEqualTo(storeId);
    }

    @Test
    void create_rejects_item_owned_by_another_store() {
        StoreId storeId = new StoreId(UUID.randomUUID());
        StoreId otherStore = new StoreId(UUID.randomUUID());

        assertThatThrownBy(() -> Combo.create(storeId, "Bad",
                Money.ofMajor(new BigDecimal("90"), UAH), null, null,
                List.of(itemOfStore(otherStore))))
                .isInstanceOf(ComboCompositionException.class)
                .hasMessageContaining("must all belong to store " + storeId.value());
    }
}
