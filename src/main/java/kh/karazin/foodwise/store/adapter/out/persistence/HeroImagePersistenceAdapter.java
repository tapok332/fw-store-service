package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.HeroImageRepository;
import kh.karazin.foodwise.store.domain.HeroImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Persistence adapter implementing the {@link HeroImageRepository} outbound port.
 */
@Component
@RequiredArgsConstructor
class HeroImagePersistenceAdapter implements HeroImageRepository {

    private final HeroImageJpaRepository heroImageJpaRepository;

    @Override
    public List<HeroImage> findAll() {
        return heroImageJpaRepository.findAll().stream()
                .map(PersistenceMappers::toHeroImage)
                .toList();
    }

    @Override
    public Optional<HeroImage> findById(Integer id) {
        return heroImageJpaRepository.findById(id).map(PersistenceMappers::toHeroImage);
    }

    @Override
    public HeroImage save(HeroImage heroImage) {
        HeroImageEntity entity = HeroImageEntity.builder()
                .id(heroImage.id())
                .key(heroImage.key())
                .url(heroImage.url())
                .icon(heroImage.icon())
                .build();
        return PersistenceMappers.toHeroImage(heroImageJpaRepository.save(entity));
    }

    @Override
    public void deleteById(Integer id) {
        heroImageJpaRepository.deleteById(id);
    }
}
