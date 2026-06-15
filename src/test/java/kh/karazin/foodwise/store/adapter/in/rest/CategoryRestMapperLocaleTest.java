package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.CategoryId;
import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryRestMapperLocaleTest {

    private final CategoryRestMapper mapper = new CategoryRestMapper();

    private Category bakery() {
        return new Category(new CategoryId(UUID.randomUUID()), "Bakery", "bakery", "croissant",
                Map.of("en", "Bakery", "uk", "Випічка"),
                Set.of(StoreType.RESTAURANT, StoreType.BAKERY));
    }

    @Test
    void toDto_returnsUkrainianName_forUkLocale() {
        CategoryDto dto = mapper.toDto(bakery(), Locale.forLanguageTag("uk"));
        assertThat(dto.name()).isEqualTo("Випічка");
    }

    @Test
    void toDto_fallsBackToEnglish_forUnknownLocale() {
        CategoryDto dto = mapper.toDto(bakery(), Locale.forLanguageTag("fr"));
        assertThat(dto.name()).isEqualTo("Bakery");
    }

    @Test
    void toDto_includesApplicableTypesAndDerivedGroups() {
        CategoryDto dto = mapper.toDto(bakery(), Locale.ENGLISH);
        assertThat(dto.applicableTypes()).containsExactlyInAnyOrder(
                StoreType.RESTAURANT, StoreType.BAKERY);
        assertThat(dto.applicableGroups()).containsExactly(StoreGroup.FOOD_SERVICE);
    }

    @Test
    void toDto_handlesNullCategory() {
        assertThat(mapper.toDto(null, Locale.ENGLISH)).isNull();
    }
}
