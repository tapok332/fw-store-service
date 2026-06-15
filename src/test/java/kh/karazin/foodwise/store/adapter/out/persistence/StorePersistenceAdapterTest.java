package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.config.StoreProperties;
import kh.karazin.foodwise.store.domain.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorePersistenceAdapterTest {

    @Mock private StoreJpaRepository storeJpaRepository;
    @Mock private CategoryJpaRepository categoryJpaRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final StoreProperties storeProperties = new StoreProperties(ZoneId.of("Europe/Kyiv"));

    private StorePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StorePersistenceAdapter(
                storeJpaRepository, categoryJpaRepository, geometryFactory, storeProperties);
    }

    @Test
    void save_newStore_buildsPointWithLngAsXAndLatAsY_andSrid4326() {
        Store store = Store.create(
                "Test Store", "desc", null, null, null, null, "addr",
                50.45, 30.52, null, null, null, null, null, null, null, null);

        when(storeJpaRepository.save(any(StoreEntity.class))).thenAnswer(inv -> {
            StoreEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        adapter.save(store);

        ArgumentCaptor<StoreEntity> captor = ArgumentCaptor.forClass(StoreEntity.class);
        verify(storeJpaRepository).save(captor.capture());
        Point loc = captor.getValue().getLocation();
        assertThat(loc.getX()).isEqualTo(30.52);   // x == lng
        assertThat(loc.getY()).isEqualTo(50.45);   // y == lat
        assertThat(loc.getSRID()).isEqualTo(4326);
    }
}
