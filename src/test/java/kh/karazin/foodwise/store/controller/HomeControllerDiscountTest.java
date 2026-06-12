package kh.karazin.foodwise.store.controller;

import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.common.money.MoneyJacksonModule;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.entity.SurpriseBoxEntity;
import kh.karazin.foodwise.store.i18n.RequestLocaleResolver;
import kh.karazin.foodwise.store.repository.SurpriseBoxRepository;
import kh.karazin.foodwise.store.service.CategoryService;
import kh.karazin.foodwise.store.service.ContentService;
import kh.karazin.foodwise.store.service.StoreService;
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
 * Standalone MockMvc test for {@link HomeController#toHomeDto} discount arithmetic.
 *
 * <p>Cases:
 * <ul>
 *     <li>(a) Normal — price 4.00 UAH, retail 5.00 UAH → discount 20%.</li>
 *     <li>(b) Null retailPrice → discount 0 (sentinel).</li>
 *     <li>(c) Price ≥ retail (surcharge / bad data) → discount clamped to 0.</li>
 * </ul>
 *
 * <p>Follows the standalone MockMvc pattern used by {@link InternalStoreControllerTest}.
 */
class HomeControllerDiscountTest {

    private static final Currency UAH = Currency.getInstance("UAH");

    private SurpriseBoxRepository boxRepo;
    private MockMvc mockMvc;
    // Jackson 3 native mapper with MoneyJacksonModule registered (needed to read Money fields)
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new MoneyJacksonModule())
            .build();

    @BeforeEach
    void setUp() {
        boxRepo = mock(SurpriseBoxRepository.class);

        HomeController controller = new HomeController(
                mock(StoreService.class),
                mock(CategoryService.class),
                mock(ContentService.class),
                boxRepo,
                mock(RequestLocaleResolver.class)
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    // ── (a) normal case ────────────────────────────────────────────────────────

    @Test
    void discount_is_20_when_price_is_80_percent_of_retail() throws Exception {
        // price = 4.00 UAH (400 minor), retail = 5.00 UAH (500 minor)
        // expected discount = round((500-400)*100.0/500) = 20
        stubBox(box(
                Money.ofMajor(new BigDecimal("4.00"), UAH),
                Money.ofMajor(new BigDecimal("5.00"), UAH)
        ));

        JsonNode first = firstBox();
        assertThat(first.path("discount").asInt()).isEqualTo(20);
    }

    // ── (b) null retailPrice → discount 0 ─────────────────────────────────────

    @Test
    void discount_is_zero_when_retailPrice_is_null() throws Exception {
        stubBox(box(
                Money.ofMajor(new BigDecimal("3.00"), UAH),
                null   // no retail price
        ));

        JsonNode first = firstBox();
        assertThat(first.path("discount").asInt()).isEqualTo(0);
    }

    // ── (c) price > retail → clamped to 0, not negative ──────────────────────

    @Test
    void discount_is_clamped_to_zero_when_price_exceeds_retail() throws Exception {
        // price 6.00 > retail 5.00 → would yield -20 without Math.max clamp
        stubBox(box(
                Money.ofMajor(new BigDecimal("6.00"), UAH),
                Money.ofMajor(new BigDecimal("5.00"), UAH)
        ));

        JsonNode first = firstBox();
        assertThat(first.path("discount").asInt())
                .as("discount must not be negative when price exceeds retail")
                .isEqualTo(0);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubBox(SurpriseBoxEntity box) {
        when(boxRepo.findNearbyBoxesWithAvailableStock(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(box));
    }

    private JsonNode firstBox() throws Exception {
        MvcResult result = mockMvc.perform(get("/home/boxes"))
                .andExpect(status().isOk())
                .andReturn();

        // use the module-aware mapper to read the response
        JsonNode root = mapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.path("data");
        assertThat(data.size()).isGreaterThan(0);
        return data.get(0);
    }

    /** Build a minimal SurpriseBoxEntity with the given price and retailPrice. */
    private SurpriseBoxEntity box(Money price, Money retailPrice) {
        StoreEntity store = StoreEntity.builder()
                .id(UUID.randomUUID())
                .name("Test Store")
                .type(StoreType.RESTAURANT)
                .address("Kyiv")
                .build();

        return SurpriseBoxEntity.builder()
                .id(UUID.randomUUID())
                .store(store)
                .title("Test Box")
                .price(price)
                .retailPrice(retailPrice)
                .stock(5)
                .deliveryAvailable(false)
                .build();
    }
}
