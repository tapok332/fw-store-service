package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.SurpriseBoxQueryRepository;
import kh.karazin.foodwise.store.domain.SurpriseBoxView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Persistence adapter implementing the {@link SurpriseBoxQueryRepository}
 * outbound port. Maps within the calling read transaction so the lazy
 * {@code store} association resolves without relying on open-session-in-view.
 */
@Component
@RequiredArgsConstructor
class SurpriseBoxQueryAdapter implements SurpriseBoxQueryRepository {

    private final SurpriseBoxJpaRepository surpriseBoxJpaRepository;

    @Override
    public List<SurpriseBoxView> findNearbyWithAvailableStock(double lat, double lon, double radius) {
        return surpriseBoxJpaRepository.findNearbyBoxesWithAvailableStock(lat, lon, radius).stream()
                .map(PersistenceMappers::toSurpriseBoxView)
                .toList();
    }
}
