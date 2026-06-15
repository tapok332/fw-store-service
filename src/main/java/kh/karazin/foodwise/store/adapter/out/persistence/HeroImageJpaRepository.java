package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for hero images. */
public interface HeroImageJpaRepository extends JpaRepository<HeroImageEntity, Integer> {
}
