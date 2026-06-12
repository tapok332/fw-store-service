package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.config.CacheConfig;
import kh.karazin.foodwise.store.dto.CategoryCreateRequest;
import kh.karazin.foodwise.store.dto.CategoryDto;
import kh.karazin.foodwise.store.entity.CategoryEntity;
import kh.karazin.foodwise.store.entity.StoreGroup;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Service for store categories. Localizes display names per request locale
 * (en fallback), and filters by {@link StoreGroup} or {@link StoreType} set
 * (mutually exclusive).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private static final String DEFAULT_LOCALE = "en";

    private final CategoryRepository categoryRepository;

    /**
     * Returns categories filtered by either {@code group} (any of its types
     * intersects {@code applicableTypes}) or by {@code types} (any of these
     * intersects {@code applicableTypes}). Passing both throws.
     *
     * <p>Localized {@code name} per {@code locale} with fallback to 'en' then
     * to canonical {@code categories.name}.
     */
    @Cacheable(value = CacheConfig.CATEGORIES_CACHE,
            key = "T(java.util.Objects).hash(#locale.language, #group, #types)")
    public List<CategoryDto> findAll(Locale locale, StoreGroup group, Set<StoreType> types) {
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
                .filter(c -> applied == null || intersects(c.getApplicableTypes(), applied))
                .map(c -> toDto(c, locale))
                .toList();
    }

    public CategoryDto findBySlug(String slug, Locale locale) {
        return categoryRepository.findBySlug(slug)
                .map(c -> toDto(c, locale))
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND,
                        "Category not found: " + slug));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CATEGORIES_CACHE, allEntries = true)
    public CategoryDto createCategory(CategoryCreateRequest request) {
        categoryRepository.findByName(request.name()).ifPresent(existing -> {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.DUPLICATE_REQUEST,
                    "Category already exists: " + request.name());
        });

        String slug = request.slug() != null && !request.slug().isBlank()
                ? request.slug()
                : slugify(request.name());

        if (categoryRepository.existsBySlug(slug)) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.DUPLICATE_REQUEST,
                    "Category slug already in use: " + slug);
        }

        CategoryEntity entity = CategoryEntity.builder()
                .name(request.name())
                .slug(slug)
                .iconName(request.iconName())
                .translations(new HashMap<>(request.translations()))
                .applicableTypes(new HashSet<>(request.applicableTypes()))
                .build();
        CategoryEntity saved = categoryRepository.save(entity);
        log.info("Created category {} (slug={}, types={}, locales={})",
                saved.getId(), saved.getSlug(),
                saved.getApplicableTypes(), saved.getTranslations().keySet());
        return toDto(saved, Locale.ENGLISH);
    }

    public CategoryDto toDto(CategoryEntity entity, Locale locale) {
        String name = resolveName(entity, locale);
        List<StoreType> types = entity.getApplicableTypes().stream()
                .sorted()
                .toList();
        List<StoreGroup> groups = entity.getApplicableTypes().stream()
                .map(StoreType::group)
                .distinct()
                .sorted()
                .toList();
        return new CategoryDto(
                entity.getId(),
                entity.getSlug(),
                name,
                entity.getIconName(),
                types,
                groups);
    }

    private static String resolveName(CategoryEntity entity, Locale locale) {
        String key = locale != null ? locale.getLanguage() : DEFAULT_LOCALE;
        String hit = entity.getTranslations().get(key);
        if (hit != null) return hit;
        String en = entity.getTranslations().get(DEFAULT_LOCALE);
        if (en != null) return en;
        return entity.getName();
    }

    private static boolean intersects(Set<StoreType> a, Set<StoreType> b) {
        return a.stream().anyMatch(b::contains);
    }

    /** {@code "Asian Fusion"} → {@code "asian-fusion"}. */
    static String slugify(String value) {
        return value == null ? null : value
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
