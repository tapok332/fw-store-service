package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.CategoryRepository;
import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.CategoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter implementing the {@link CategoryRepository} outbound port.
 */
@Component
@RequiredArgsConstructor
class CategoryPersistenceAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public List<Category> findAll() {
        return categoryJpaRepository.findAll().stream()
                .map(PersistenceMappers::toCategory)
                .toList();
    }

    @Override
    public Optional<Category> findById(CategoryId categoryId) {
        return categoryJpaRepository.findById(categoryId.value()).map(PersistenceMappers::toCategory);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return categoryJpaRepository.findByName(name).map(PersistenceMappers::toCategory);
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return categoryJpaRepository.findBySlug(slug).map(PersistenceMappers::toCategory);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return categoryJpaRepository.existsBySlug(slug);
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = CategoryEntity.builder()
                .name(category.name())
                .slug(category.slug())
                .iconName(category.iconName())
                .translations(new HashMap<>(category.translations()))
                .applicableTypes(new HashSet<>(category.applicableTypes()))
                .build();
        return PersistenceMappers.toCategory(categoryJpaRepository.save(entity));
    }
}
