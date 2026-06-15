package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for store menu items with search capability. */
public interface StoreMenuItemJpaRepository extends JpaRepository<StoreMenuItemEntity, UUID> {

    List<StoreMenuItemEntity> findByStoreId(UUID storeId);

    Page<StoreMenuItemEntity> findByStoreId(UUID storeId, Pageable pageable);

    @Query("SELECT m FROM StoreMenuItemEntity m WHERE m.store.id = :storeId " +
            "AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<StoreMenuItemEntity> searchByStoreIdAndQuery(@Param("storeId") UUID storeId,
                                                      @Param("query") String query);

    List<StoreMenuItemEntity> findByStoreIdAndAvailableTrue(UUID storeId);
}
