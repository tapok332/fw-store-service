package kh.karazin.foodwise.store.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for stores with PostGIS spatial queries.
 */
public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID>, JpaSpecificationExecutor<StoreEntity> {

    /**
     * Find stores near a given point within a radius (in meters).
     */
    @Query(value = """
            SELECT s.*, ST_Distance(s.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) AS distance
            FROM stores s
            WHERE ST_DWithin(s.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius)
            ORDER BY distance
            """, nativeQuery = true)
    List<StoreEntity> findNearbyStores(@Param("lat") double lat,
                                       @Param("lon") double lon,
                                       @Param("radius") double radius);

    /**
     * Distance in meters from a given point to the store's location.
     */
    @Query(value = """
            SELECT ST_Distance(s.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography)
            FROM stores s
            WHERE s.id = :storeId
            """, nativeQuery = true)
    Optional<Double> findDistanceMeters(@Param("storeId") UUID storeId,
                                        @Param("lat") double lat,
                                        @Param("lon") double lon);
}
