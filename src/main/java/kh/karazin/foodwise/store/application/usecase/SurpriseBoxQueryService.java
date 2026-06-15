package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.store.application.port.in.SurpriseBoxQueryUseCase;
import kh.karazin.foodwise.store.application.port.out.SurpriseBoxQueryRepository;
import kh.karazin.foodwise.store.domain.SurpriseBoxView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Surprise-box discovery for the home screen.
 */
@Service
@RequiredArgsConstructor
class SurpriseBoxQueryService implements SurpriseBoxQueryUseCase {

    private final SurpriseBoxQueryRepository surpriseBoxQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SurpriseBoxView> findNearbyForHome(double lat, double lng, double radius) {
        return surpriseBoxQueryRepository.findNearbyWithAvailableStock(lat, lng, radius);
    }
}
