package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.common.money.MoneyJacksonModule;
import kh.karazin.foodwise.store.application.port.in.CategoryUseCase;
import kh.karazin.foodwise.store.application.port.in.ContentUseCase;
import kh.karazin.foodwise.store.application.port.in.StoreUseCase;
import kh.karazin.foodwise.store.application.port.in.SurpriseBoxQueryUseCase;
import kh.karazin.foodwise.store.domain.SurpriseBoxView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc test for the {@code /home/boxes} discount arithmetic, now
 * computed by {@link SurpriseBoxView#discountPercentage()} and rendered by
 * {@link SurpriseBoxRestMapper}.
 *
 * <ul>
 *     <li>(a) Normal — price 4.00 UAH, retail 5.00 UAH → discount 20%.</li>
 *     <li>(b) Null retailPrice → discount 0 (sentinel).</li>
 *     <li>(c) Price ≥ retail (surcharge / bad data) → discount clamped to 0.</li>
 * </ul>
 */
class HomeControllerDiscountTest {

    private static final Currency UAH = Currency.getInstance("UAH");

    private SurpriseBoxQueryUseCase surpriseBoxQueryUseCase;
    private MockMvc mockMvc;
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new MoneyJacksonModule())
            .build();

    @BeforeEach
    void setUp() {
        surpriseBoxQueryUseCase = mock(SurpriseBoxQueryUseCase.class);

        HomeController controller = new HomeController(
                mock(StoreUseCase.class),
                mock(CategoryUseCase.class),
                mock(ContentUseCase.class),
                surpriseBoxQueryUseCase,
                mock(RequestLocaleResolver.class),
                mock(StoreRestMapper.class),
                mock(CategoryRestMapper.class),
                new SurpriseBoxRestMapper(),
                mock(ContentRestMapper.class));

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void discount_is_20_when_price_is_80_percent_of_retail() throws Exception {
        stubBox(box(
                Money.ofMajor(new BigDecimal("4.00"), UAH),
                Money.ofMajor(new BigDecimal("5.00"), UAH)));

        assertThat(firstBox().path("discount").asInt()).isEqualTo(20);
    }

    @Test
    void discount_is_zero_when_retailPrice_is_null() throws Exception {
        stubBox(box(Money.ofMajor(new BigDecimal("3.00"), UAH), null));

        assertThat(firstBox().path("discount").asInt()).isEqualTo(0);
    }

    @Test
    void discount_is_clamped_to_zero_when_price_exceeds_retail() throws Exception {
        stubBox(box(
                Money.ofMajor(new BigDecimal("6.00"), UAH),
                Money.ofMajor(new BigDecimal("5.00"), UAH)));

        assertThat(firstBox().path("discount").asInt())
                .as("discount must not be negative when price exceeds retail")
                .isEqualTo(0);
    }

    private void stubBox(SurpriseBoxView box) {
        when(surpriseBoxQueryUseCase.findNearbyForHome(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(box));
    }

    private JsonNode firstBox() throws Exception {
        MvcResult result = mockMvc.perform(get("/home/boxes"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = mapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.size()).isGreaterThan(0);
        return data.get(0);
    }

    private SurpriseBoxView box(Money price, Money retailPrice) {
        return new SurpriseBoxView(
                UUID.randomUUID(), UUID.randomUUID(), "Test Box", "desc", "img",
                price, retailPrice, 5, null, null, false, "category", null,
                "Test Store", "Kyiv", null, null);
    }
}
