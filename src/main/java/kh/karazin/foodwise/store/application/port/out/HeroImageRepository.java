package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.HeroImage;

import java.util.List;
import java.util.Optional;

/** Outbound port for hero image persistence. */
public interface HeroImageRepository {

    List<HeroImage> findAll();

    Optional<HeroImage> findById(Integer id);

    HeroImage save(HeroImage heroImage);

    void deleteById(Integer id);
}
