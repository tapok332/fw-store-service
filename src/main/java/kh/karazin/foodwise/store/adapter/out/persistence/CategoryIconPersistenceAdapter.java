package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.CategoryIconRepository;
import kh.karazin.foodwise.store.domain.CategoryIcon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter implementing the {@link CategoryIconRepository} outbound port.
 */
@Component
@RequiredArgsConstructor
class CategoryIconPersistenceAdapter implements CategoryIconRepository {

    private final CategoryIconJpaRepository categoryIconJpaRepository;

    @Override
    public List<CategoryIcon> findAll() {
        return categoryIconJpaRepository.findAll().stream()
                .map(PersistenceMappers::toCategoryIcon)
                .toList();
    }

    @Override
    public Optional<CategoryIcon> findById(Integer id) {
        return categoryIconJpaRepository.findById(id).map(PersistenceMappers::toCategoryIcon);
    }

    @Override
    public CategoryIcon save(CategoryIcon categoryIcon) {
        CategoryIconEntity entity = CategoryIconEntity.builder()
                .id(categoryIcon.id())
                .key(categoryIcon.key())
                .iconName(categoryIcon.iconName())
                .build();
        return PersistenceMappers.toCategoryIcon(categoryIconJpaRepository.save(entity));
    }

    @Override
    public void deleteById(Integer id) {
        categoryIconJpaRepository.deleteById(id);
    }
}
