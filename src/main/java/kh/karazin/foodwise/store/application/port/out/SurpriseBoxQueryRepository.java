package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.SurpriseBoxView;

import java.util.List;

/** Outbound port for surprise-box read projections (home discovery). */
public interface SurpriseBoxQueryRepository {

    List<SurpriseBoxView> findNearbyWithAvailableStock(double lat, double lon, double radius);
}
