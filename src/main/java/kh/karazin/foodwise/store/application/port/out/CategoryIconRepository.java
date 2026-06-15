package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.CategoryIcon;

import java.util.List;
import java.util.Optional;

/** Outbound port for category icon persistence. */
public interface CategoryIconRepository {

    List<CategoryIcon> findAll();

    Optional<CategoryIcon> findById(Integer id);

    CategoryIcon save(CategoryIcon categoryIcon);

    void deleteById(Integer id);
}
