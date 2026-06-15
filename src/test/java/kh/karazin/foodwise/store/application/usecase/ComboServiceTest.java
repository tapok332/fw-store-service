package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.store.application.port.in.CreateComboCommand;
import kh.karazin.foodwise.store.application.port.out.ComboRepository;
import kh.karazin.foodwise.store.application.port.out.MenuItemRepository;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.domain.Combo;
import kh.karazin.foodwise.store.domain.ComboId;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.StoreId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComboServiceTest {

    private static final Currency UAH = Currency.getInstance("UAH");

    @Mock private ComboRepository comboRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private MenuItemRepository menuItemRepository;

    private ComboService comboService;

    @BeforeEach
    void setUp() {
        comboService = new ComboService(comboRepository, storeRepository, menuItemRepository);
    }

    private MenuItem itemOf(UUID storeId) {
        return new MenuItem(new MenuItemId(UUID.randomUUID()), "Item", null,
                Money.ofMajor(new BigDecimal("50"), UAH), null, null, true, new StoreId(storeId), null);
    }

    @Test
    void createCombo_persistsComboWithMenuItems_whenStoreAndItemsExist() {
        UUID storeId = UUID.randomUUID();
        var command = new CreateComboCommand(storeId, "Burger Combo",
                Money.ofMajor(new BigDecimal("199.00"), UAH), null, new BigDecimal("31.00"),
                List.of(UUID.randomUUID(), UUID.randomUUID()));

        when(storeRepository.existsById(new StoreId(storeId))).thenReturn(true);
        when(menuItemRepository.findAllById(any()))
                .thenReturn(List.of(itemOf(storeId), itemOf(storeId)));
        when(comboRepository.save(any(Combo.class)))
                .thenAnswer(inv -> withId(inv.getArgument(0)));

        Combo result = comboService.createCombo(command);

        assertThat(result.id()).isNotNull();
        assertThat(result.title()).isEqualTo("Burger Combo");
        assertThat(result.items()).hasSize(2);
        assertThat(result.storeId()).isEqualTo(new StoreId(storeId));
    }

    @Test
    void createCombo_throwsEntityNotFound_whenStoreDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        var command = new CreateComboCommand(storeId, "Test",
                Money.ofMajor(new BigDecimal("100.00"), UAH), null, null,
                List.of(UUID.randomUUID()));
        when(storeRepository.existsById(new StoreId(storeId))).thenReturn(false);

        assertThatThrownBy(() -> comboService.createCombo(command))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(comboRepository, never()).save(any());
    }

    @Test
    void createCombo_throwsInvalidRequest_whenMenuItemBelongsToDifferentStore() {
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        var command = new CreateComboCommand(storeId, "Bad Combo",
                Money.ofMajor(new BigDecimal("100.00"), UAH), null, null,
                List.of(UUID.randomUUID()));

        when(storeRepository.existsById(new StoreId(storeId))).thenReturn(true);
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(itemOf(otherStoreId)));

        assertThatThrownBy(() -> comboService.createCombo(command))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(comboRepository, never()).save(any());
    }

    @Test
    void createCombo_throwsInvalidRequest_whenMenuItemDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        var command = new CreateComboCommand(storeId, "Bad Combo",
                Money.ofMajor(new BigDecimal("100.00"), UAH), null, null,
                List.of(UUID.randomUUID()));

        when(storeRepository.existsById(new StoreId(storeId))).thenReturn(true);
        when(menuItemRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> comboService.createCombo(command))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(comboRepository, never()).save(any());
    }

    private static Combo withId(Combo c) {
        return new Combo(new ComboId(UUID.randomUUID()), c.storeId(), c.title(), c.price(),
                c.imageUrl(), c.savings(), c.items());
    }
}
