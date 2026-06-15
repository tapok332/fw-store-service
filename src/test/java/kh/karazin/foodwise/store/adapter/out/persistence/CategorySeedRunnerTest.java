package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.domain.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorySeedRunnerTest {

    @Mock
    private CategoryJpaRepository categoryRepository;

    private CategorySeedRunner runner;

    @BeforeEach
    void setUp() {
        runner = new CategorySeedRunner(categoryRepository);
    }

    @Test
    void run_insertsCategoryWithTranslationsAndApplicableTypes_whenSlugMissing() {
        when(categoryRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(CategoryEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        runner.run();

        ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(categoryRepository, atLeastOnce()).save(captor.capture());

        CategoryEntity pizza = captor.getAllValues().stream()
                .filter(c -> "pizza".equals(c.getSlug()))
                .findFirst().orElseThrow();
        assertThat(pizza.getTranslations()).containsEntry("en", "Pizza");
        assertThat(pizza.getTranslations()).containsEntry("uk", "Піца");
        assertThat(pizza.getApplicableTypes()).containsExactly(StoreType.RESTAURANT);
    }

    @Test
    void run_mergesMissingTranslationsAndTypes_whenSlugAlreadyExists() {
        UUID pizzaId = UUID.randomUUID();
        CategoryEntity existing = CategoryEntity.builder()
                .id(pizzaId)
                .slug("pizza")
                .name("Pizza")
                .translations(new HashMap<>(Map.of("en", "Pizza")))   // missing 'uk'
                .applicableTypes(new HashSet<>())                     // missing RESTAURANT
                .build();
        when(categoryRepository.findBySlug(any())).thenAnswer(inv ->
                "pizza".equals(inv.getArgument(0))
                        ? Optional.of(existing)
                        : Optional.empty());
        when(categoryRepository.save(any(CategoryEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        runner.run();

        assertThat(existing.getTranslations()).containsEntry("uk", "Піца");
        assertThat(existing.getApplicableTypes()).contains(StoreType.RESTAURANT);
        verify(categoryRepository, atLeastOnce()).save(existing);
    }

    @Test
    void run_seedsBothFoodServiceAndRetailCategories() {
        when(categoryRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(CategoryEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        runner.run();

        ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(categoryRepository, atLeastOnce()).save(captor.capture());

        Set<String> savedSlugs = new HashSet<>();
        captor.getAllValues().forEach(c -> savedSlugs.add(c.getSlug()));

        assertThat(savedSlugs).contains("pizza", "sushi", "bakery", "asian", "burgers",
                "coffee", "dessert", "vegan", "pastry", "greek");
        assertThat(savedSlugs).contains("produce", "dairy", "meat", "seafood", "frozen",
                "snacks", "candy", "chocolate");
    }

    @Test
    void run_skipsCompletelyWhenNothingMissing() {
        when(categoryRepository.findBySlug(any())).thenAnswer(inv -> {
            String slug = inv.getArgument(0);
            CategoryEntity full = CategoryEntity.builder()
                    .id(UUID.randomUUID())
                    .slug(slug)
                    .name(slug)
                    .translations(new HashMap<>(Map.of("en", slug, "uk", slug)))
                    .applicableTypes(new HashSet<>(Set.of(StoreType.RESTAURANT,
                            StoreType.CAFE, StoreType.BAKERY,
                            StoreType.GROCERY, StoreType.SWEETS)))
                    .build();
            return Optional.of(full);
        });

        runner.run();

        verify(categoryRepository, never()).save(any());
    }
}
