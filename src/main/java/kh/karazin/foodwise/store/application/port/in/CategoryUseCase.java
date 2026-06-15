package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;

import java.util.List;
import java.util.Set;

/** Category listing and management. Localization is applied by the inbound adapter. */
public interface CategoryUseCase {

    /**
     * Returns categories filtered by either {@code group} or {@code types}
     * (mutually exclusive). Passing both throws {@link IllegalArgumentException}.
     */
    List<Category> findAll(StoreGroup group, Set<StoreType> types);

    Category findBySlug(String slug);

    Category createCategory(CreateCategoryCommand command);
}
