package kh.karazin.foodwise.store.controller;

import kh.karazin.foodwise.common.dto.internal.InternalLocationDto;
import kh.karazin.foodwise.common.dto.internal.InternalMenuItemDto;
import kh.karazin.foodwise.common.dto.internal.InternalStoreDto;
import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.common.exception.GlobalExceptionHandler;
import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.common.money.MoneyJacksonModule;
import kh.karazin.foodwise.store.service.StoreMenuItemService;
import kh.karazin.foodwise.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
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
 * convention for slice tests in Spring Boot 4.
 *
 * <p>Locks the contract:
 * <ul>
 *     <li>{@code GET /internal/stores/{id}} returns {@link InternalStoreDto}
 *         (6 fields), no envelope, no leaked menu/combo/rating data.</li>
 *     <li>{@code GET /internal/menu-items/{id}} returns
 *         {@link InternalMenuItemDto} (6 fields), no envelope.</li>
 *     <li>Missing resource maps to HTTP 404.</li>
 * </ul>
 */
class InternalStoreControllerTest {

    private static final Currency UAH = Currency.getInstance("UAH");

    private StoreService storeService;
    private StoreMenuItemService menuItemService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storeService = mock(StoreService.class);
        menuItemService = mock(StoreMenuItemService.class);

        // Standalone MockMvc serializes Money via a Jackson 3 converter carrying the
        // MoneyJacksonModule — the same wire form ({"amount":"...","currency":"..."})
        // every service produces in the running context.
        var moneyConverter = new JacksonJsonHttpMessageConverter(
                JsonMapper.builder().addModule(new MoneyJacksonModule()).build());

        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalStoreController(storeService, menuItemService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(moneyConverter)
                .build();
    }

    @Test
    void getStore_returns_typed_InternalStoreDto_without_menuItems_or_combos() throws Exception {
        UUID storeId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        // deliveryFee=35.00 UAH, minOrderAmount=100.00 UAH: Money on the wire
        // serializes to {"amount":"35.00","currency":"UAH"}.
        InternalStoreDto dto = new InternalStoreDto(
                storeId,
                "Bagels & Co",
                "https://cdn.example.com/img.png",
                new InternalLocationDto(50.0044, 36.2350),
                Money.ofMajor(new BigDecimal("35.00"), UAH),
                Money.ofMajor(new BigDecimal("100.00"), UAH)
        );
        when(storeService.getStoreForInternal(eq(storeId))).thenReturn(dto);

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
                // forbidden public-DTO fields MUST NOT leak
                .andExpect(jsonPath("$.menuItems").doesNotExist())
                .andExpect(jsonPath("$.combos").doesNotExist())
                .andExpect(jsonPath("$.distance").doesNotExist())
                .andExpect(jsonPath("$.opensAt").doesNotExist())
                .andExpect(jsonPath("$.closesAt").doesNotExist())
                .andExpect(jsonPath("$.rating").doesNotExist())
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.type").doesNotExist())
                // no envelope per ADR 0010
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void getStore_returns_404_when_store_missing() throws Exception {
        UUID storeId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(storeService.getStoreForInternal(eq(storeId)))
                .thenThrow(FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));

        mockMvc.perform(get("/internal/stores/{storeId}", storeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMenuItem_returns_typed_InternalMenuItemDto_without_description() throws Exception {
        UUID itemId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID storeId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        // Stored menu prices are whole-hryvnia majors → 120.00 UAH on the wire.
        InternalMenuItemDto dto = new InternalMenuItemDto(
                itemId, "Bagel", Money.ofMajor(new BigDecimal("120.00"), UAH),
                "https://cdn.example.com/bagel.png", storeId, true);
        when(menuItemService.getMenuItemForInternal(eq(itemId))).thenReturn(dto);

        mockMvc.perform(get("/internal/menu-items/{itemId}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.name").value("Bagel"))
                .andExpect(jsonPath("$.price.amount").value("120.00"))
                .andExpect(jsonPath("$.price.currency").value("UAH"))
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.available").value(true))
                // forbidden public-DTO fields
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.legacyCategory").doesNotExist())
                .andExpect(jsonPath("$.sectionId").doesNotExist())
                // no envelope
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void getMenuItem_returns_404_when_item_missing() throws Exception {
        UUID itemId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(menuItemService.getMenuItemForInternal(eq(itemId)))
                .thenThrow(FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Menu item not found: " + itemId));

        mockMvc.perform(get("/internal/menu-items/{itemId}", itemId))
                .andExpect(status().isNotFound());
    }
}
