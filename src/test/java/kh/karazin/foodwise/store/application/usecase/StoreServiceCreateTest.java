package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.CreateStoreCommand;
import kh.karazin.foodwise.store.application.port.out.CategoryRepository;
import kh.karazin.foodwise.store.application.port.out.ComboRepository;
import kh.karazin.foodwise.store.application.port.out.MenuItemRepository;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.CategoryId;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceCreateTest {

    @Mock private StoreRepository storeRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private ComboRepository comboRepository;
    @Mock private CategoryRepository categoryRepository;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(
                storeRepository, menuItemRepository, comboRepository, categoryRepository);
    }

    private static CreateStoreCommand command(UUID categoryId) {
        return new CreateStoreCommand(
                "Test Store", "desc", null, null, null, categoryId, "addr",
                new BigDecimal("50.45"), new BigDecimal("30.52"),
                null, null, null, null, null, null, null, null);
    }

    @Test
    void createStore_persistsStore_andReturnsSavedWithId() {
        when(storeRepository.save(any(Store.class))).thenAnswer(inv -> withId(inv.getArgument(0)));

        Store result = storeService.createStore(command(null));

        assertThat(result.id()).isNotNull();
        assertThat(result.name()).isEqualTo("Test Store");
        assertThat(result.latitude()).isEqualTo(50.45);
        assertThat(result.longitude()).isEqualTo(30.52);
        assertThat(result.category()).isNull();
    }

    @Test
    void createStore_throwsEntityNotFound_whenCategoryIdDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(categoryRepository.findById(new CategoryId(missingId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.createStore(command(missingId)))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(storeRepository, never()).save(any());
    }

    @Test
    void createStore_resolvesCategory_whenCategoryIdProvided() {
        UUID categoryId = UUID.randomUUID();
        Category bakery = new Category(new CategoryId(categoryId), "Bakery", "bakery", null,
                Map.of("en", "Bakery"), Set.of());
        when(categoryRepository.findById(new CategoryId(categoryId))).thenReturn(Optional.of(bakery));
        when(storeRepository.save(any(Store.class))).thenAnswer(inv -> withId(inv.getArgument(0)));

        Store result = storeService.createStore(command(categoryId));

        assertThat(result.category()).isNotNull();
        assertThat(result.category().name()).isEqualTo("Bakery");
    }

    private static Store withId(Store s) {
        return new Store(new StoreId(UUID.randomUUID()), s.name(), s.description(), s.imageUrl(),
                s.heroImageUrl(), s.type(), s.category(), s.address(), s.latitude(), s.longitude(),
                s.rating(), s.opensAt(), s.closesAt(), s.phone(), s.website(),
                s.deliveryFee(), s.minOrderAmount(), s.priceLevel());
    }
}
