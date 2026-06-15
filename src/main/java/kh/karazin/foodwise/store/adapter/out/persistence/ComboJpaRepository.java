package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for combos. */
public interface ComboJpaRepository extends JpaRepository<ComboEntity, UUID> {

    List<ComboEntity> findByStoreId(UUID storeId);
}
