package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for surprise boxes with spatial queries. */
public interface SurpriseBoxJpaRepository extends JpaRepository<SurpriseBoxEntity, UUID> {

    /**
     * Find surprise boxes near a given point with available stock.
     */
    @Query(value = """
            SELECT sb.*
            FROM surprise_boxes sb
            JOIN stores s ON sb.store_id = s.id
            WHERE sb.stock > 0
              AND ST_DWithin(s.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius)
            ORDER BY ST_Distance(s.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography)
            """, nativeQuery = true)
    List<SurpriseBoxEntity> findNearbyBoxesWithAvailableStock(@Param("lat") double lat,
                                                              @Param("lon") double lon,
                                                              @Param("radius") double radius);
}
