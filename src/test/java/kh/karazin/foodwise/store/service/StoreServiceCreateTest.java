package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.config.StoreProperties;
import kh.karazin.foodwise.store.dto.StoreCreateRequest;
import kh.karazin.foodwise.store.dto.StoreDto;
import kh.karazin.foodwise.store.entity.CategoryEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.mapper.StoreMapper;
import kh.karazin.foodwise.store.repository.CategoryRepository;
import kh.karazin.foodwise.store.repository.ComboRepository;
import kh.karazin.foodwise.store.repository.StoreMenuItemRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceCreateTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StoreMenuItemRepository menuItemRepository;
    @Mock private ComboRepository comboRepository;
    @Mock private CategoryRepository categoryRepository;

    private final StoreProperties storeProperties = new StoreProperties(ZoneId.of("Europe/Kyiv"));
    private final StoreMapper storeMapper = new StoreMapper(storeProperties);
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(
                storeRepository, menuItemRepository, comboRepository,
                categoryRepository, storeMapper, geometryFactory, storeProperties);
    }

    @Test
    void createStore_buildsPointWithLngAsXAndLatAsY_andPersistsStore() {
        var request = new StoreCreateRequest(
                "Test Store", "desc", null, null, null, null, "addr",
                new BigDecimal("50.45"),   // lat (Kyiv)
                new BigDecimal("30.52"),   // lng
                null, null, null, null, null, null, null, null);

        when(storeRepository.save(any(StoreEntity.class))).thenAnswer(inv -> {
            StoreEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        StoreDto result = storeService.createStore(request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Store");

        ArgumentCaptor<StoreEntity> captor = ArgumentCaptor.forClass(StoreEntity.class);
        verify(storeRepository).save(captor.capture());
        Point loc = captor.getValue().getLocation();
        assertThat(loc.getX()).isEqualTo(30.52);   // x == lng
        assertThat(loc.getY()).isEqualTo(50.45);   // y == lat
        assertThat(loc.getSRID()).isEqualTo(4326);
    }

    @Test
    void createStore_throwsEntityNotFound_whenCategoryIdDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        var request = new StoreCreateRequest(
                "Test", null, null, null, null, missingId, "addr",
                new BigDecimal("0"), new BigDecimal("0"),
                null, null, null, null, null, null, null, null);
        when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.createStore(request))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(storeRepository, never()).save(any());
    }

    @Test
    void createStore_succeedsWithoutCategory_whenCategoryIdIsNull() {
        var request = new StoreCreateRequest(
                "Test", null, null, null, null, null, "addr",
                new BigDecimal("0"), new BigDecimal("0"),
                null, null, null, null, null, null, null, null);
        when(storeRepository.save(any(StoreEntity.class))).thenAnswer(inv -> {
            StoreEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        StoreDto result = storeService.createStore(request);

        assertThat(result).isNotNull();
        assertThat(result.categoryName()).isNull();
        verify(categoryRepository, never()).findById(isNull());
    }

    @Test
    void createStore_resolvesCategory_whenCategoryIdProvided() {
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = CategoryEntity.builder().id(categoryId).name("Bakery").slug("bakery").build();
        var request = new StoreCreateRequest(
                "Test", null, null, null, null, categoryId, "addr",
                new BigDecimal("0"), new BigDecimal("0"),
                null, null, null, null, null, null, null, null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(storeRepository.save(any(StoreEntity.class))).thenAnswer(inv -> {
            StoreEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        StoreDto result = storeService.createStore(request);

        assertThat(result.categoryName()).isEqualTo("Bakery");
    }
}
