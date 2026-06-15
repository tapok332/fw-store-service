package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.domain.StoreType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static kh.karazin.foodwise.store.domain.StoreType.BAKERY;
import static kh.karazin.foodwise.store.domain.StoreType.CAFE;
import static kh.karazin.foodwise.store.domain.StoreType.GROCERY;
import static kh.karazin.foodwise.store.domain.StoreType.RESTAURANT;
import static kh.karazin.foodwise.store.domain.StoreType.SWEETS;

/**
 * Seeds canonical categories with stable slugs, per-locale display names, and
 * applicable {@link StoreType}s.
 *
 * <p><b>Idempotent.</b> On every boot:
 * <ul>
 *   <li>If slug missing — inserts the full entity (translations + types).</li>
 *   <li>If slug exists — merges <i>missing</i> translations and types only.
 *       Existing rows are preserved (no overwrites, no deletes — preserves
 *       any admin-added customizations).</li>
 * </ul>
 *
 * <p>Disabled in tests via {@code @Profile("!test")}. Lives in the persistence
 * adapter because it performs entity-level merges on managed rows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(0)
@Profile("!test")
class CategorySeedRunner implements CommandLineRunner {

    private final CategoryJpaRepository categoryRepository;

    /** Canonical list. Adding a locale or a type here propagates on next boot. */
    private static final List<Seed> CANONICAL = List.of(
            // FOOD_SERVICE — cuisine
            new Seed("pizza",     "Pizza",     "pizza",     Set.of(RESTAURANT),
                    Map.of("en", "Pizza",     "uk", "Піца")),
            new Seed("sushi",     "Sushi",     "fish",      Set.of(RESTAURANT),
                    Map.of("en", "Sushi",     "uk", "Суші")),
            new Seed("bakery",    "Bakery",    "croissant", Set.of(RESTAURANT, BAKERY),
                    Map.of("en", "Bakery",    "uk", "Випічка")),
            new Seed("asian",     "Asian",     "noodles",   Set.of(RESTAURANT),
                    Map.of("en", "Asian",     "uk", "Азійська")),
            new Seed("burgers",   "Burgers",   "burger",    Set.of(RESTAURANT),
                    Map.of("en", "Burgers",   "uk", "Бургери")),
            new Seed("coffee",    "Coffee",    "coffee",    Set.of(RESTAURANT, CAFE),
                    Map.of("en", "Coffee",    "uk", "Кава")),
            new Seed("dessert",   "Dessert",   "cake",      Set.of(RESTAURANT),
                    Map.of("en", "Dessert",   "uk", "Десерти")),
            new Seed("vegan",     "Vegan",     "leaf",      Set.of(RESTAURANT),
                    Map.of("en", "Vegan",     "uk", "Веганське")),
            new Seed("pastry",    "Pastry",    "cookie",    Set.of(RESTAURANT, BAKERY),
                    Map.of("en", "Pastry",    "uk", "Кондитерські")),
            new Seed("greek",     "Greek",     "olive",     Set.of(RESTAURANT),
                    Map.of("en", "Greek",     "uk", "Грецька")),

            // RETAIL — departments
            new Seed("produce",   "Produce",   "apple",     Set.of(GROCERY),
                    Map.of("en", "Produce",   "uk", "Овочі та фрукти")),
            new Seed("dairy",     "Dairy",     "milk",      Set.of(GROCERY),
                    Map.of("en", "Dairy",     "uk", "Молочні продукти")),
            new Seed("meat",      "Meat",      "drumstick", Set.of(GROCERY),
                    Map.of("en", "Meat",      "uk", "М'ясо")),
            new Seed("seafood",   "Seafood",   "fish",      Set.of(GROCERY),
                    Map.of("en", "Seafood",   "uk", "Морепродукти")),
            new Seed("frozen",    "Frozen",    "snowflake", Set.of(GROCERY),
                    Map.of("en", "Frozen",    "uk", "Заморожене")),
            new Seed("snacks",    "Snacks",    "popcorn",   Set.of(GROCERY, SWEETS),
                    Map.of("en", "Snacks",    "uk", "Снеки")),
            new Seed("candy",     "Candy",     "candy",     Set.of(SWEETS),
                    Map.of("en", "Candy",     "uk", "Цукерки")),
            new Seed("chocolate", "Chocolate", "chocolate", Set.of(SWEETS),
                    Map.of("en", "Chocolate", "uk", "Шоколад"))
    );

    @Override
    @Transactional
    public void run(String... args) {
        int inserted = 0;
        int merged = 0;
        for (Seed seed : CANONICAL) {
            Optional<CategoryEntity> existing = categoryRepository.findBySlug(seed.slug());
            if (existing.isEmpty()) {
                categoryRepository.save(CategoryEntity.builder()
                        .slug(seed.slug())
                        .name(seed.name())
                        .iconName(seed.iconName())
                        .translations(new HashMap<>(seed.i18n()))
                        .applicableTypes(new HashSet<>(seed.applicableTypes()))
                        .build());
                inserted++;
                continue;
            }
            CategoryEntity entity = existing.get();
            boolean changed = false;
            for (Map.Entry<String, String> e : seed.i18n().entrySet()) {
                if (!entity.getTranslations().containsKey(e.getKey())) {
                    entity.getTranslations().put(e.getKey(), e.getValue());
                    changed = true;
                }
            }
            for (StoreType t : seed.applicableTypes()) {
                if (!entity.getApplicableTypes().contains(t)) {
                    entity.getApplicableTypes().add(t);
                    changed = true;
                }
            }
            if (changed) {
                categoryRepository.save(entity);
                merged++;
            }
        }
        if (inserted > 0 || merged > 0) {
            log.info("CategorySeedRunner: inserted={} merged={}", inserted, merged);
        }
    }

    private record Seed(String slug,
                        String name,
                        String iconName,
                        Set<StoreType> applicableTypes,
                        Map<String, String> i18n) {}
}
