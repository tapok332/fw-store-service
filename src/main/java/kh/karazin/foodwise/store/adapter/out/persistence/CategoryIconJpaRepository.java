package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for category icons. */
public interface CategoryIconJpaRepository extends JpaRepository<CategoryIconEntity, Integer> {
}
