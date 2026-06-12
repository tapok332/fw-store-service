package kh.karazin.foodwise.store.mapper;

import kh.karazin.foodwise.store.dto.CategoryDto;
import kh.karazin.foodwise.store.entity.CategoryEntity;
import kh.karazin.foodwise.store.entity.StoreGroup;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.config.StoreProperties;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperLocaleTest {

    private final StoreMapper mapper = new StoreMapper(
            new StoreProperties(ZoneId.of("Europe/Kyiv")));

    private CategoryEntity bakery() {
        return CategoryEntity.builder()
                .id(UUID.randomUUID())
                .slug("bakery")
                .name("Bakery")
                .iconName("croissant")
                .translations(new HashMap<>(Map.of("en", "Bakery", "uk", "Випічка")))
                .applicableTypes(new HashSet<>(Set.of(StoreType.RESTAURANT, StoreType.BAKERY)))
                .build();
    }

    @Test
    void toCategoryDto_returnsUkrainianName_forUkLocale() {
        CategoryDto dto = mapper.toCategoryDto(bakery(), Locale.forLanguageTag("uk"));
        assertThat(dto.name()).isEqualTo("Випічка");
    }

    @Test
    void toCategoryDto_fallsBackToEnglish_forUnknownLocale() {
        CategoryDto dto = mapper.toCategoryDto(bakery(), Locale.forLanguageTag("fr"));
        assertThat(dto.name()).isEqualTo("Bakery");
    }

    @Test
    void toCategoryDto_includesApplicableTypesAndDerivedGroups() {
        CategoryDto dto = mapper.toCategoryDto(bakery(), Locale.ENGLISH);
        assertThat(dto.applicableTypes()).containsExactlyInAnyOrder(
                StoreType.RESTAURANT, StoreType.BAKERY);
        assertThat(dto.applicableGroups()).containsExactly(StoreGroup.FOOD_SERVICE);
    }

    @Test
    void toCategoryDto_handlesNullEntity() {
        assertThat(mapper.toCategoryDto((CategoryEntity) null, Locale.ENGLISH)).isNull();
    }
}
