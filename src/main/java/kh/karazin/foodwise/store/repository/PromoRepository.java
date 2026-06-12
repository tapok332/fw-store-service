package kh.karazin.foodwise.store.repository;

import kh.karazin.foodwise.store.entity.PromoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for store promotions.
 */
public interface PromoRepository extends JpaRepository<PromoEntity, UUID> {

    List<PromoEntity> findByStoreIdAndActiveTrueOrderByPriorityDesc(UUID storeId);

    List<PromoEntity> findByStoreId(UUID storeId);
}
