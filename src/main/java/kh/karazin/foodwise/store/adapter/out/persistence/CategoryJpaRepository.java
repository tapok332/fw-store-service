package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for store categories. */
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findByName(String name);

    Optional<CategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
