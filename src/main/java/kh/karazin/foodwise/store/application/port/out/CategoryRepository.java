package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.CategoryId;

import java.util.List;
import java.util.Optional;

/** Outbound port for category persistence. */
public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findById(CategoryId categoryId);

    Optional<Category> findByName(String name);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Category save(Category category);
}
