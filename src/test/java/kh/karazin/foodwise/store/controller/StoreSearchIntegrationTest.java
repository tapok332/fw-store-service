package kh.karazin.foodwise.store.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import kh.karazin.foodwise.store.entity.CategoryEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.repository.CategoryRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import kh.karazin.foodwise.common.money.Money;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the full {@code GET /stores} query-string contract:
 * search, categoryId, latitude/longitude, minRating, maxDistance, openNow,
 * priceLevel (multi-valued), sort, page/limit.
 *
 * <p>Backed by a real PostGIS container — the {@code ST_*} functions and the
 * geography type are not faithfully simulated by H2.
 */
@Testcontainers
@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "internal.service.secret=test-internal-secret",
        "foodwise.store.timezone=Europe/Kyiv",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class StoreSearchIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4-alpine")
                    .asCompatibleSubstituteFor("postgres"))
            .withReuse(true);

    @LocalServerPort private int port;
    @Autowired private JsonMapper objectMapper;
    @Autowired private StoreRepository storeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private GeometryFactory geometryFactory;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Reference point (Kyiv center) used as the "user" location for distance/sort tests. */
    private static final double USER_LAT = 50.450;
    private static final double USER_LNG = 30.520;

    private UUID italianId;

    @BeforeEach
    void seed() {
        storeRepository.deleteAll();
        categoryRepository.deleteAll();

        CategoryEntity asian = categoryRepository.save(CategoryEntity.builder()
                .name("Asian").slug("asian").iconName("noodles").build());
        CategoryEntity italian = categoryRepository.save(CategoryEntity.builder()
                .name("Italian").slug("italian").iconName("pasta").build());
        italianId = italian.getId();

        // baseline: open 24/7 so openNow assertions stay deterministic across CI clock times
        save("Sakura",        StoreType.RESTAURANT, asian,   4.8, 2, USER_LAT,         USER_LNG,            LocalTime.of(0, 0),  LocalTime.of(23, 59));
        save("Cheap Noodles", StoreType.RESTAURANT, asian,   2.5, 1, USER_LAT + 0.001, USER_LNG + 0.001,    LocalTime.of(0, 0),  LocalTime.of(23, 59));
        save("Roma",          StoreType.RESTAURANT, italian, 4.2, 3, USER_LAT,         USER_LNG + 0.0005,   LocalTime.of(0, 0),  LocalTime.of(23, 59));
        save("Far Pizza",     StoreType.RESTAURANT, italian, 4.0, 2, USER_LAT + 0.05,  USER_LNG + 0.10,     LocalTime.of(0, 0),  LocalTime.of(23, 59));
        // Tiny one-minute window — vanishingly small chance the CI clock lands inside.
        save("Closed Sushi",  StoreType.BAKERY,     asian,   4.9, 4, USER_LAT,         USER_LNG,            LocalTime.of(3, 0),  LocalTime.of(3, 1));
        // RETAIL store — required for group=RETAIL filter tests
        save("Fresh Market",  StoreType.GROCERY,    asian,   3.8, 2, USER_LAT,         USER_LNG + 0.0003,   LocalTime.of(0, 0),  LocalTime.of(23, 59));
    }

    @Test
    void filtersByMinRating() throws Exception {
        JsonNode content = call("/stores?minRating=4.5").path("data").path("content");

        assertThat(content.size()).isGreaterThanOrEqualTo(2);
        for (JsonNode node : content) {
            assertThat(node.path("rating").decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("4.5"));
        }
    }

    @Test
    void filtersByCategoryId() throws Exception {
        JsonNode content = call("/stores?categoryId=" + italianId).path("data").path("content");

        assertThat(content).hasSize(2);
        for (JsonNode node : content) {
            assertThat(node.path("category").path("slug").asText()).isEqualTo("italian");
            assertThat(node.path("category").path("name").asText()).isEqualTo("Italian");
            assertThat(node.path("categoryName").asText()).isEqualTo("Italian");   // deprecated mirror
        }
    }

    @Test
    void filtersByCategorySlug() throws Exception {
        JsonNode content = call("/stores?categorySlug=asian").path("data").path("content");

        assertThat(content.size()).isGreaterThanOrEqualTo(2);
        for (JsonNode node : content) {
            assertThat(node.path("category").path("slug").asText()).isEqualTo("asian");
        }
    }

    @Test
    void filtersByType() throws Exception {
        JsonNode restaurants = call("/stores?type=RESTAURANT").path("data").path("content");
        JsonNode bakeries    = call("/stores?type=BAKERY").path("data").path("content");

        assertThat(restaurants.size()).isEqualTo(4);
        assertThat(bakeries.size()).isEqualTo(1);
        for (JsonNode node : restaurants) assertThat(node.path("type").asText()).isEqualTo("RESTAURANT");
        for (JsonNode node : bakeries)    assertThat(node.path("type").asText()).isEqualTo("BAKERY");
    }

    @Test
    void invalidSortValueReturns400() throws Exception {
        // sort=password is not in the whitelist — must be rejected, not 500'd, not order-by'd
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/stores?sort=password"))
                .header("X-Internal-Token", "test-internal-secret")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void combinedFilters_typeAndCategorySlugAndMinRating() throws Exception {
        JsonNode content = call("/stores?type=RESTAURANT&categorySlug=asian&minRating=4.0&sort=rating")
                .path("data").path("content");

        // Sakura (4.8) is the only Asian restaurant with rating ≥ 4.0
        assertThat(content).hasSize(1);
        JsonNode sample = content.get(0);
        assertThat(sample.path("type").asText()).isEqualTo("RESTAURANT");
        assertThat(sample.path("category").path("slug").asText()).isEqualTo("asian");
        assertThat(sample.path("rating").decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("4.0"));
    }

    @Test
    void filtersByPriceLevelMultiValued() throws Exception {
        // Sakura(2), Cheap Noodles(1), Far Pizza(2), Fresh Market(2) match. Roma(3) + Closed Sushi(4) excluded.
        JsonNode content = call("/stores?priceLevel=1&priceLevel=2").path("data").path("content");

        assertThat(content).hasSize(4);
        for (JsonNode node : content) {
            int level = node.path("priceLevel").asInt();
            assertThat(level).isIn(1, 2);
        }
    }

    @Test
    void filtersByMaxDistance() throws Exception {
        // 1 km radius — Far Pizza is ~8 km out, so 5 close stores remain (incl. Fresh Market GROCERY).
        JsonNode content = call("/stores?latitude=%s&longitude=%s&maxDistance=1"
                .formatted(USER_LAT, USER_LNG)).path("data").path("content");

        assertThat(content).hasSize(5);
        for (JsonNode node : content) {
            assertThat(node.path("name").asText()).isNotEqualTo("Far Pizza");
        }
    }

    @Test
    void sortsByRatingDescending() throws Exception {
        JsonNode content = call("/stores?sort=rating").path("data").path("content");

        BigDecimal previous = null;
        for (JsonNode node : content) {
            BigDecimal current = node.path("rating").decimalValue();
            if (previous != null) {
                assertThat(current.compareTo(previous)).isLessThanOrEqualTo(0);
            }
            previous = current;
        }
    }

    @Test
    void sortsByDistance_farthestStoreIsLast() throws Exception {
        JsonNode content = call("/stores?latitude=%s&longitude=%s&sort=distance"
                .formatted(USER_LAT, USER_LNG)).path("data").path("content");

        assertThat(content.size()).isGreaterThan(1);
        assertThat(content.get(content.size() - 1).path("name").asText()).isEqualTo("Far Pizza");
    }

    @Test
    void filtersByOpenNow_excludesNarrowEarlyMorningWindow() throws Exception {
        JsonNode content = call("/stores?openNow=true").path("data").path("content");

        // baseline stores are open 00:00-23:59 → always present
        // Closed Sushi 03:00-03:01 → effectively always excluded
        for (JsonNode node : content) {
            assertThat(node.path("name").asText()).isNotEqualTo("Closed Sushi");
        }
    }

    @Test
    void nearbyBranchReturnsRealData_notNull() throws Exception {
        // Regression guard for the old "ApiResponse.success(null)" branch.
        JsonNode body = call("/stores?latitude=%s&longitude=%s".formatted(USER_LAT, USER_LNG));

        assertThat(body.path("data").isMissingNode()).isFalse();
        assertThat(body.path("data").path("content").size()).isGreaterThan(0);
    }

    @Test
    void limitParamCapsPageSize_andReportsTrueTotalElements() throws Exception {
        JsonNode data = call("/stores?limit=2").path("data");

        assertThat(data.path("content")).hasSize(2);
        assertThat(data.path("totalElements").asInt()).isEqualTo(6);
    }

    @Test
    void searchParamMatchesNameOrDescription() throws Exception {
        JsonNode content = call("/stores?search=Sakura").path("data").path("content");

        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("name").asText()).isEqualTo("Sakura");
    }

    @Test
    void searchStores_filtersByGroupFoodService() throws Exception {
        JsonNode body = call("/stores?group=FOOD_SERVICE");
        JsonNode content = body.get("data").get("content");

        assertThat(content.size()).isGreaterThan(0);
        for (JsonNode store : content) {
            String t = store.get("type").asText();
            assertThat(t).isIn("RESTAURANT", "CAFE", "BAKERY");
        }
    }

    @Test
    void searchStores_filtersByGroupRetail() throws Exception {
        JsonNode body = call("/stores?group=RETAIL");
        JsonNode content = body.get("data").get("content");

        assertThat(content.size()).isGreaterThan(0);
        for (JsonNode store : content) {
            String t = store.get("type").asText();
            assertThat(t).isIn("GROCERY", "SWEETS", "OTHER");
        }
    }

    @Test
    void searchStores_returns400_onBothTypeAndGroup() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/stores?type=RESTAURANT&group=FOOD_SERVICE"))
                .header("X-Internal-Token", "test-internal-secret")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("message").asText())
                .contains("mutually exclusive");
    }

    // ---------- helpers ----------

    private JsonNode call(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + url))
                .header("X-Internal-Token", "test-internal-secret")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as("status for %s, body=%s", url, response.body())
                .isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    /**
     * Verifies that a non-null {@code deliveryFee} (a {@link Money} field) is
     * serialized in the 1b wire form {@code {"amount":"...","currency":"..."}}
     * and NOT as a raw numeric {@code amountMinor}.  This proves that
     * {@code MoneyJacksonConfig} is active in the running Spring context.
     */
    @Test
    void storeDto_deliveryFee_serializes_as_amount_plus_currency_not_amountMinor() throws Exception {
        // Seed one store with a known delivery fee
        CategoryEntity cat = categoryRepository.save(CategoryEntity.builder()
                .name("Fee Test").slug("fee-test").iconName("fee").build());
        saveWithFee("FeeStore", StoreType.RESTAURANT, cat, 4.0, 2,
                USER_LAT, USER_LNG, LocalTime.of(0, 0), LocalTime.of(23, 59),
                Money.ofMajor(new BigDecimal("3.50"), Currency.getInstance("UAH")));

        JsonNode content = call("/stores?search=FeeStore").path("data").path("content");
        assertThat(content).hasSize(1);

        JsonNode deliveryFee = content.get(0).path("deliveryFee");
        // Must be an object with "amount" and "currency" — NOT a scalar
        assertThat(deliveryFee.isObject())
                .as("deliveryFee must be a JSON object {amount, currency}")
                .isTrue();
        assertThat(deliveryFee.path("amount").asText())
                .as("amount should be 3.50")
                .isEqualTo("3.50");
        assertThat(deliveryFee.path("currency").asText())
                .as("currency should be UAH")
                .isEqualTo("UAH");
        // Guard: the raw internal field must NOT be present at the top level
        assertThat(deliveryFee.has("amountMinor"))
                .as("amountMinor must not appear in wire form")
                .isFalse();
    }

    private void save(String name, StoreType type, CategoryEntity category, double rating, int priceLevel,
                      double lat, double lng, LocalTime opensAt, LocalTime closesAt) {
        saveWithFee(name, type, category, rating, priceLevel, lat, lng, opensAt, closesAt, null);
    }

    private void saveWithFee(String name, StoreType type, CategoryEntity category, double rating, int priceLevel,
                              double lat, double lng, LocalTime opensAt, LocalTime closesAt, Money deliveryFee) {
        Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        storeRepository.save(StoreEntity.builder()
                .name(name)
                .description(name + " description")
                .type(type)
                .category(category)
                .rating(BigDecimal.valueOf(rating))
                .priceLevel(priceLevel)
                .location(point)
                .opensAt(opensAt)
                .closesAt(closesAt)
                .address("test addr")
                .deliveryFee(deliveryFee)
                .build());
    }
}
