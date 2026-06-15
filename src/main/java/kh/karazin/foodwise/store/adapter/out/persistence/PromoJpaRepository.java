package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for store promotions. */
public interface PromoJpaRepository extends JpaRepository<PromoEntity, UUID> {

    List<PromoEntity> findByStoreIdAndActiveTrueOrderByPriorityDesc(UUID storeId);

    List<PromoEntity> findByStoreId(UUID storeId);
}
