package kh.karazin.foodwise.store.repository;

import kh.karazin.foodwise.store.entity.StoreReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for store reviews.
 */
public interface StoreReviewRepository extends JpaRepository<StoreReviewEntity, UUID> {
}
