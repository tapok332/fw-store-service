package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.CreateCategoryCommand;
import kh.karazin.foodwise.store.application.port.out.CategoryRepository;
import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.CategoryId;
import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
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

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    private static Category pizza() {
        return new Category(new CategoryId(UUID.randomUUID()), "Pizza", "pizza", "pizza",
                Map.of("en", "Pizza", "uk", "Піца"), Set.of(StoreType.RESTAURANT));
    }

    private static Category produce() {
        return new Category(new CategoryId(UUID.randomUUID()), "Produce", "produce", "apple",
                Map.of("en", "Produce"), Set.of(StoreType.GROCERY));
    }

    private static Category bakery() {
        return new Category(new CategoryId(UUID.randomUUID()), "Bakery", "bakery", "croissant",
                Map.of("en", "Bakery"), Set.of(StoreType.RESTAURANT, StoreType.BAKERY));
    }

    @Test
    void findAll_filtersByGroup() {
        when(categoryRepository.findAll()).thenReturn(List.of(pizza(), produce()));

        List<Category> result = categoryService.findAll(StoreGroup.RETAIL, null);

        assertThat(result).extracting(Category::slug).containsExactly("produce");
    }

    @Test
    void findAll_filtersByTypes() {
        when(categoryRepository.findAll()).thenReturn(List.of(pizza(), bakery()));

        List<Category> result = categoryService.findAll(null, Set.of(StoreType.BAKERY));

        assertThat(result).extracting(Category::slug).containsExactly("bakery");
    }

    @Test
    void findAll_returnsAll_whenNoFilter() {
        when(categoryRepository.findAll()).thenReturn(List.of(pizza(), produce()));

        List<Category> result = categoryService.findAll(null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void findAll_throwsWhenBothGroupAndTypesProvided() {
        assertThatThrownBy(() -> categoryService.findAll(StoreGroup.RETAIL, Set.of(StoreType.BAKERY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    void findBySlug_returnsCategory() {
        Category pizza = pizza();
        when(categoryRepository.findBySlug("pizza")).thenReturn(Optional.of(pizza));

        Category result = categoryService.findBySlug("pizza");

        assertThat(result.applicableTypes()).containsExactly(StoreType.RESTAURANT);
    }

    @Test
    void findBySlug_throwsNotFound_whenMissing() {
        when(categoryRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findBySlug("nope"))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createCategory_persistsTranslationsAndTypes() {
        var command = new CreateCategoryCommand(
                "Produce", "produce", "apple",
                Set.of(StoreType.GROCERY),
                Map.of("en", "Produce", "uk", "Овочі та фрукти"));
        when(categoryRepository.findByName("Produce")).thenReturn(Optional.empty());
        when(categoryRepository.existsBySlug("produce")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(inv -> withId(inv.getArgument(0)));

        Category result = categoryService.createCategory(command);

        assertThat(result.slug()).isEqualTo("produce");
        assertThat(result.applicableTypes()).containsExactly(StoreType.GROCERY);
    }

    @Test
    void createCategory_rejectsDuplicateName() {
        var command = new CreateCategoryCommand(
                "Bakery", "bakery", null,
                Set.of(StoreType.BAKERY),
                Map.of("en", "Bakery"));
        when(categoryRepository.findByName("Bakery")).thenReturn(Optional.of(bakery()));

        assertThatThrownBy(() -> categoryService.createCategory(command))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.CONFLICT);
        verify(categoryRepository, never()).save(any());
    }

    private static Category withId(Category c) {
        return new Category(new CategoryId(UUID.randomUUID()), c.name(), c.slug(), c.iconName(),
                c.translations(), c.applicableTypes());
    }
}
