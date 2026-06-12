package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.store.config.StoreProperties;
import kh.karazin.foodwise.store.dto.ComboCreateRequest;
import kh.karazin.foodwise.store.dto.ComboDto;
import kh.karazin.foodwise.store.entity.ComboEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreMenuItemEntity;
import kh.karazin.foodwise.store.mapper.StoreMapper;
import kh.karazin.foodwise.store.repository.ComboRepository;
import kh.karazin.foodwise.store.repository.StoreMenuItemRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComboServiceTest {

    @Mock private ComboRepository comboRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreMenuItemRepository menuItemRepository;

    private final StoreMapper storeMapper = new StoreMapper(new StoreProperties(ZoneId.of("Europe/Kyiv")));

    private ComboService comboService;

    @BeforeEach
    void setUp() {
        comboService = new ComboService(
                comboRepository, storeRepository, menuItemRepository, storeMapper);
    }

    @Test
    void createCombo_persistsComboWithMenuItems_whenStoreAndItemsExist() {
        UUID storeId = UUID.randomUUID();
        UUID itemId1 = UUID.randomUUID();
        UUID itemId2 = UUID.randomUUID();
        StoreEntity store = StoreEntity.builder().id(storeId).name("Bakery").build();
        Currency uah = Currency.getInstance("UAH");
        StoreMenuItemEntity item1 = StoreMenuItemEntity.builder()
                .id(itemId1).name("Burger").price(Money.ofMajor(new BigDecimal("150"), uah)).store(store).available(true).build();
        StoreMenuItemEntity item2 = StoreMenuItemEntity.builder()
                .id(itemId2).name("Fries").price(Money.ofMajor(new BigDecimal("80"), uah)).store(store).available(true).build();

        var request = new ComboCreateRequest(
                "Burger Combo",
                Money.ofMajor(new BigDecimal("199.00"), Currency.getInstance("UAH")),
                null,
                new BigDecimal("31.00"),
                List.of(itemId1, itemId2));

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(menuItemRepository.findAllById(List.of(itemId1, itemId2)))
                .thenReturn(List.of(item1, item2));
        when(comboRepository.save(any(ComboEntity.class))).thenAnswer(inv -> {
            ComboEntity c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ComboDto result = comboService.createCombo(storeId, request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Burger Combo");
        assertThat(result.price()).isEqualTo(Money.ofMajor(new BigDecimal("199.00"), Currency.getInstance("UAH")));
        assertThat(result.menuItems()).hasSize(2);

        ArgumentCaptor<ComboEntity> captor = ArgumentCaptor.forClass(ComboEntity.class);
        verify(comboRepository).save(captor.capture());
        assertThat(captor.getValue().getStore().getId()).isEqualTo(storeId);
        assertThat(captor.getValue().getMenuItems()).extracting(StoreMenuItemEntity::getId)
                .containsExactlyInAnyOrder(itemId1, itemId2);
    }

    @Test
    void createCombo_throwsEntityNotFound_whenStoreDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        var request = new ComboCreateRequest(
                "Test",
                Money.ofMajor(new BigDecimal("100.00"), Currency.getInstance("UAH")),
                null, null,
                List.of(UUID.randomUUID()));
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comboService.createCombo(storeId, request))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(comboRepository, never()).save(any());
    }

    @Test
    void createCombo_throwsInvalidRequest_whenMenuItemBelongsToDifferentStore() {
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        StoreEntity store = StoreEntity.builder().id(storeId).name("Bakery").build();
        StoreEntity otherStore = StoreEntity.builder().id(otherStoreId).name("Other").build();
        StoreMenuItemEntity foreignItem = StoreMenuItemEntity.builder()
                .id(itemId).name("Foreign").price(Money.ofMajor(new BigDecimal("50"), Currency.getInstance("UAH"))).store(otherStore).available(true).build();

        var request = new ComboCreateRequest(
                "Bad Combo",
                Money.ofMajor(new BigDecimal("100.00"), Currency.getInstance("UAH")),
                null, null, List.of(itemId));

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(menuItemRepository.findAllById(List.of(itemId))).thenReturn(List.of(foreignItem));

        assertThatThrownBy(() -> comboService.createCombo(storeId, request))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(comboRepository, never()).save(any());
    }

    @Test
    void createCombo_throwsInvalidRequest_whenMenuItemDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        UUID missingItemId = UUID.randomUUID();
        StoreEntity store = StoreEntity.builder().id(storeId).name("Bakery").build();

        var request = new ComboCreateRequest(
                "Bad Combo",
                Money.ofMajor(new BigDecimal("100.00"), Currency.getInstance("UAH")),
                null, null,
                List.of(missingItemId));

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(menuItemRepository.findAllById(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> comboService.createCombo(storeId, request))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(comboRepository, never()).save(any());
    }
}
