package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.MenuSectionRepository;
import kh.karazin.foodwise.store.domain.MenuSection;
import kh.karazin.foodwise.store.domain.MenuSectionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter implementing the {@link MenuSectionRepository} outbound port.
 */
@Component
@RequiredArgsConstructor
class MenuSectionPersistenceAdapter implements MenuSectionRepository {

    private final MenuSectionJpaRepository sectionJpaRepository;
    private final StoreJpaRepository storeJpaRepository;

    @Override
    public MenuSection save(MenuSection section) {
        MenuSectionEntity entity = MenuSectionEntity.builder()
                .store(storeJpaRepository.getReferenceById(section.storeId().value()))
                .title(section.title())
                .sortOrder(section.sortOrder())
                .build();
        MenuSectionEntity saved = sectionJpaRepository.save(entity);
        return new MenuSection(
                new MenuSectionId(saved.getId()),
                section.storeId(),
                saved.getTitle(),
                saved.getSortOrder());
    }
}
