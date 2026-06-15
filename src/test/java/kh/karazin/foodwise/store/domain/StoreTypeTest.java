package kh.karazin.foodwise.store.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreTypeTest {

    @Test
    void every_type_has_non_null_group() {
        for (StoreType t : StoreType.values()) {
            assertThat(t.group())
                    .as("group() for %s", t)
                    .isNotNull();
        }
    }

    @Test
    void restaurant_cafe_bakery_belong_to_food_service() {
        assertThat(StoreType.RESTAURANT.group()).isEqualTo(StoreGroup.FOOD_SERVICE);
        assertThat(StoreType.CAFE.group()).isEqualTo(StoreGroup.FOOD_SERVICE);
        assertThat(StoreType.BAKERY.group()).isEqualTo(StoreGroup.FOOD_SERVICE);
    }

    @Test
    void grocery_sweets_other_belong_to_retail() {
        assertThat(StoreType.GROCERY.group()).isEqualTo(StoreGroup.RETAIL);
        assertThat(StoreType.SWEETS.group()).isEqualTo(StoreGroup.RETAIL);
        assertThat(StoreType.OTHER.group()).isEqualTo(StoreGroup.RETAIL);
    }

    @Test
    void typesIn_food_service_returns_R_C_B() {
        Set<StoreType> result = StoreType.typesIn(StoreGroup.FOOD_SERVICE);
        assertThat(result).containsExactlyInAnyOrder(
                StoreType.RESTAURANT, StoreType.CAFE, StoreType.BAKERY);
    }

    @Test
    void typesIn_retail_returns_G_S_O() {
        Set<StoreType> result = StoreType.typesIn(StoreGroup.RETAIL);
        assertThat(result).containsExactlyInAnyOrder(
                StoreType.GROCERY, StoreType.SWEETS, StoreType.OTHER);
    }

    @Test
    void typesIn_returns_unmodifiable_set() {
        Set<StoreType> result = StoreType.typesIn(StoreGroup.RETAIL);
        assertThatThrownBy(() -> result.add(StoreType.RESTAURANT))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void every_group_has_at_least_one_type() {
        for (StoreGroup g : StoreGroup.values()) {
            assertThat(StoreType.typesIn(g))
                    .as("typesIn(%s)", g)
                    .isNotEmpty();
        }
    }
}
