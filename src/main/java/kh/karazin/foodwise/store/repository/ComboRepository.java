package kh.karazin.foodwise.store.repository;

import kh.karazin.foodwise.store.entity.ComboEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for combos.
 */
public interface ComboRepository extends JpaRepository<ComboEntity, UUID> {

    List<ComboEntity> findByStoreId(UUID storeId);
}
