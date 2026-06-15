package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data repository for store reviews. */
public interface StoreReviewJpaRepository extends JpaRepository<StoreReviewEntity, UUID> {
}
