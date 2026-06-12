package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.dto.CategoryCreateRequest;
import kh.karazin.foodwise.store.dto.CategoryDto;
import kh.karazin.foodwise.store.entity.CategoryEntity;
import kh.karazin.foodwise.store.entity.StoreGroup;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static CategoryEntity pizza() {
        return CategoryEntity.builder()
                .id(UUID.randomUUID())
                .slug("pizza")
                .name("Pizza")
                .iconName("pizza")
                .translations(new HashMap<>(Map.of("en", "Pizza", "uk", "Піца")))
                .applicableTypes(new HashSet<>(Set.of(StoreType.RESTAURANT)))
                .build();
    }

    @Test
    void findAll_resolvesNameByLocale() {
        when(categoryRepository.findAll()).thenReturn(List.of(pizza()));

        List<CategoryDto> result = categoryService.findAll(
                Locale.forLanguageTag("uk"), null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Піца");
    }

    @Test
    void findAll_fallsBackToEnglish_whenRequestedLocaleMissing() {
        when(categoryRepository.findAll()).thenReturn(List.of(pizza()));

        List<CategoryDto> result = categoryService.findAll(
                Locale.forLanguageTag("fr"), null, null);

        assertThat(result.get(0).name()).isEqualTo("Pizza");
    }

    @Test
    void findAll_filtersByGroup() {
        CategoryEntity produce = CategoryEntity.builder()
                .id(UUID.randomUUID()).slug("produce").name("Produce")
                .translations(new HashMap<>(Map.of("en", "Produce")))
                .applicableTypes(new HashSet<>(Set.of(StoreType.GROCERY)))
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(pizza(), produce));

        List<CategoryDto> result = categoryService.findAll(
                Locale.ENGLISH, StoreGroup.RETAIL, null);

        assertThat(result).extracting(CategoryDto::slug).containsExactly("produce");
    }

    @Test
    void findAll_filtersByTypes() {
        CategoryEntity bakery = CategoryEntity.builder()
                .id(UUID.randomUUID()).slug("bakery").name("Bakery")
                .translations(new HashMap<>(Map.of("en", "Bakery")))
                .applicableTypes(new HashSet<>(Set.of(StoreType.RESTAURANT, StoreType.BAKERY)))
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(pizza(), bakery));

        List<CategoryDto> result = categoryService.findAll(
                Locale.ENGLISH, null, Set.of(StoreType.BAKERY));

        assertThat(result).extracting(CategoryDto::slug).containsExactly("bakery");
    }

    @Test
    void findAll_throwsWhenBothGroupAndTypesProvided() {
        assertThatThrownBy(() -> categoryService.findAll(
                Locale.ENGLISH, StoreGroup.RETAIL, Set.of(StoreType.BAKERY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    void findBySlug_resolvesNameByLocale() {
        when(categoryRepository.findBySlug("pizza")).thenReturn(Optional.of(pizza()));

        CategoryDto result = categoryService.findBySlug("pizza", Locale.forLanguageTag("uk"));

        assertThat(result.name()).isEqualTo("Піца");
        assertThat(result.applicableTypes()).containsExactly(StoreType.RESTAURANT);
        assertThat(result.applicableGroups()).containsExactly(StoreGroup.FOOD_SERVICE);
    }

    @Test
    void createCategory_persistsTranslationsAndTypes() {
        var request = new CategoryCreateRequest(
                "Produce", "produce", "apple",
                Set.of(StoreType.GROCERY),
                Map.of("en", "Produce", "uk", "Овочі та фрукти"));
        when(categoryRepository.findByName("Produce")).thenReturn(Optional.empty());
        when(categoryRepository.existsBySlug("produce")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class)))
                .thenAnswer(inv -> {
                    CategoryEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        CategoryDto result = categoryService.createCategory(request);

        assertThat(result.name()).isEqualTo("Produce");          // default locale = en
        assertThat(result.applicableTypes()).containsExactly(StoreType.GROCERY);
    }

    @Test
    void createCategory_rejectsDuplicateName() {
        var request = new CategoryCreateRequest(
                "Bakery", "bakery", null,
                Set.of(StoreType.BAKERY),
                Map.of("en", "Bakery"));
        when(categoryRepository.findByName("Bakery"))
                .thenReturn(Optional.of(CategoryEntity.builder()
                        .id(UUID.randomUUID()).name("Bakery").build()));

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.CONFLICT);
        verify(categoryRepository, never()).save(any());
    }
}
