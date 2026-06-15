package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.SurpriseBoxView;

import java.util.List;

/** Surprise-box discovery for the home screen. */
public interface SurpriseBoxQueryUseCase {

    List<SurpriseBoxView> findNearbyForHome(double lat, double lng, double radius);
}
