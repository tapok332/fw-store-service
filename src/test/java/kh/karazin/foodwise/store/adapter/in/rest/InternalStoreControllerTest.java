package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.common.exception.GlobalExceptionHandler;
import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.common.money.MoneyJacksonModule;
import kh.karazin.foodwise.store.application.port.in.MenuItemUseCase;
import kh.karazin.foodwise.store.application.port.in.StoreUseCase;
import kh.karazin.foodwise.store.config.StoreProperties;
import kh.karazin.foodwise.store.domain.CatalogItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreId;
import kh.karazin.foodwise.store.domain.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link InternalStoreController}.
 *
 * <p>Standalone {@code MockMvc} (no Spring context) per ADR 0010 / project
 * convention for slice tests in Spring Boot 4. Locks the internal wire contract:
 * typed {@code Internal*Dto} (no envelope), 404 on missing resources.
 */
class InternalStoreControllerTest {

    private static final Currency UAH = Currency.getInstance("UAH");

    private StoreUseCase storeUseCase;
    private MenuItemUseCase menuItemUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storeUseCase = mock(StoreUseCase.class);
        menuItemUseCase = mock(MenuItemUseCase.class);
        StoreRestMapper storeRestMapper = new StoreRestMapper(
                new StoreProperties(ZoneId.of("Europe/Kyiv")), new CategoryRestMapper());

        var moneyConverter = new JacksonJsonHttpMessageConverter(
                JsonMapper.builder().addModule(new MoneyJacksonModule()).build());

        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalStoreController(storeUseCase, menuItemUseCase, storeRestMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(moneyConverter)
                .build();
    }

    @Test
    void getStore_returns_typed_InternalStoreDto_without_menuItems_or_combos() throws Exception {
        UUID storeId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Store store = new Store(
                new StoreId(storeId), "Bagels & Co", null, "https://cdn.example.com/img.png", null,
                StoreType.RESTAURANT, null, "addr", 50.0044, 36.2350, null, null, null, null, null,
                Money.ofMajor(new BigDecimal("35.00"), UAH),
                Money.ofMajor(new BigDecimal("100.00"), UAH), null);
        when(storeUseCase.getStoreForInternal(eq(new StoreId(storeId)))).thenReturn(store);

        mockMvc.perform(get("/internal/stores/{storeId}", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storeId.toString()))
                .andExpect(jsonPath("$.name").value("Bagels & Co"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/img.png"))
                .andExpect(jsonPath("$.location.latitude").value(50.0044))
                .andExpect(jsonPath("$.location.longitude").value(36.2350))
                .andExpect(jsonPath("$.deliveryFee.amount").value("35.00"))
                .andExpect(jsonPath("$.deliveryFee.currency").value("UAH"))
                .andExpect(jsonPath("$.minOrderAmount.amount").value("100.00"))
                .andExpect(jsonPath("$.minOrderAmount.currency").value("UAH"))
                .andExpect(jsonPath("$.menuItems").doesNotExist())
                .andExpect(jsonPath("$.combos").doesNotExist())
                .andExpect(jsonPath("$.distance").doesNotExist())
                .andExpect(jsonPath("$.opensAt").doesNotExist())
                .andExpect(jsonPath("$.closesAt").doesNotExist())
                .andExpect(jsonPath("$.rating").doesNotExist())
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void getStore_returns_404_when_store_missing() throws Exception {
        UUID storeId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(storeUseCase.getStoreForInternal(eq(new StoreId(storeId))))
                .thenThrow(FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));

        mockMvc.perform(get("/internal/stores/{storeId}", storeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMenuItem_returns_typed_InternalMenuItemDto_without_description() throws Exception {
        UUID itemId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID storeId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        CatalogItem item = new CatalogItem(itemId, "Bagel",
                Money.ofMajor(new BigDecimal("120.00"), UAH), "https://cdn.example.com/bagel.png",
                storeId, true);
        when(menuItemUseCase.getMenuItemForInternal(eq(new MenuItemId(itemId)))).thenReturn(item);

        mockMvc.perform(get("/internal/menu-items/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.name").value("Bagel"))
                .andExpect(jsonPath("$.price.amount").value("120.00"))
                .andExpect(jsonPath("$.price.currency").value("UAH"))
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.legacyCategory").doesNotExist())
                .andExpect(jsonPath("$.sectionId").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void getMenuItem_returns_404_when_item_missing() throws Exception {
        UUID itemId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(menuItemUseCase.getMenuItemForInternal(eq(new MenuItemId(itemId))))
                .thenThrow(FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId));

        mockMvc.perform(get("/internal/menu-items/{itemId}", itemId))
                .andExpect(status().isNotFound());
    }
}
