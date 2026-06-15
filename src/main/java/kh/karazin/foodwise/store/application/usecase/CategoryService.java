package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.CacheNames;
import kh.karazin.foodwise.store.application.port.in.CategoryUseCase;
import kh.karazin.foodwise.store.application.port.in.CreateCategoryCommand;
import kh.karazin.foodwise.store.application.port.out.CategoryRepository;
import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Category listing and management. Localization of display names is applied by
 * the inbound REST adapter, per request locale.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CategoryService implements CategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Override
    @Cacheable(value = CacheNames.CATEGORIES,
            key = "T(java.util.Objects).hash(#group, #types)")
    public List<Category> findAll(StoreGroup group, Set<StoreType> types) {
        if (group != null && types != null && !types.isEmpty()) {
            throw new IllegalArgumentException(
                    "type and group are mutually exclusive");
        }

        Set<StoreType> filter = null;
        if (group != null) {
            filter = StoreType.typesIn(group);
        } else if (types != null && !types.isEmpty()) {
            filter = types;
        }
        final Set<StoreType> applied = filter;

        return categoryRepository.findAll().stream()
                .filter(c -> applied == null || c.appliesToAnyOf(applied))
                .toList();
    }

    @Override
    public Category findBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND,
                        "Category not found: " + slug));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.CATEGORIES, allEntries = true)
    public Category createCategory(CreateCategoryCommand command) {
        categoryRepository.findByName(command.name()).ifPresent(existing -> {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.DUPLICATE_REQUEST,
                    "Category already exists: " + command.name());
        });

        String slug = command.slug() != null && !command.slug().isBlank()
                ? command.slug()
                : Category.slugify(command.name());

        if (categoryRepository.existsBySlug(slug)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.DUPLICATE_REQUEST,
                    "Category slug already in use: " + slug);
        }

        Category category = Category.create(
                command.name(),
                slug,
                command.iconName(),
                new HashMap<>(command.translations()),
                new HashSet<>(command.applicableTypes()));
        Category saved = categoryRepository.save(category);
        log.info("Created category {} (slug={}, types={}, locales={})",
                saved.id().value(), saved.slug(),
                saved.applicableTypes(), saved.translations().keySet());
        return saved;
    }
}
