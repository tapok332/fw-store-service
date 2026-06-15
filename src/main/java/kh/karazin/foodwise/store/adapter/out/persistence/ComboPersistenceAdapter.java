package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.application.port.out.ComboRepository;
import kh.karazin.foodwise.store.domain.Combo;
import kh.karazin.foodwise.store.domain.ComboId;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence adapter implementing the {@link ComboRepository} outbound port.
 */
@Component
@RequiredArgsConstructor
class ComboPersistenceAdapter implements ComboRepository {

    private final ComboJpaRepository comboJpaRepository;
    private final StoreJpaRepository storeJpaRepository;
    private final StoreMenuItemJpaRepository menuItemJpaRepository;

    @Override
    public List<Combo> findByStoreId(StoreId storeId) {
        return comboJpaRepository.findByStoreId(storeId.value()).stream()
                .map(PersistenceMappers::toCombo)
                .toList();
    }

    @Override
    public Combo save(Combo combo) {
        List<StoreMenuItemEntity> itemRefs = new ArrayList<>();
        for (MenuItem item : combo.items()) {
            itemRefs.add(menuItemJpaRepository.getReferenceById(item.id().value()));
        }
        ComboEntity entity = ComboEntity.builder()
                .store(storeJpaRepository.getReferenceById(combo.storeId().value()))
                .title(combo.title())
                .price(combo.price())
                .imageUrl(combo.imageUrl())
                .savings(combo.savings())
                .menuItems(itemRefs)
                .build();
        ComboEntity saved = comboJpaRepository.save(entity);
        // Return with the generated id, reusing the already-resolved domain items
        // (avoids reloading the lazy ManyToMany proxies for the response).
        return new Combo(
                new ComboId(saved.getId()),
                combo.storeId(),
                combo.title(),
                combo.price(),
                combo.imageUrl(),
                combo.savings(),
                combo.items());
    }
}
