package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.store.adapter.out.persistence.CategoryEntity;
import kh.karazin.foodwise.store.adapter.out.persistence.CategoryJpaRepository;
import kh.karazin.foodwise.store.adapter.out.persistence.StoreEntity;
import kh.karazin.foodwise.store.adapter.out.persistence.StoreJpaRepository;
import kh.karazin.foodwise.store.domain.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
@ActiveProfiles("test")
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
    @Autowired private StoreJpaRepository storeRepository;
    @Autowired private CategoryJpaRepository categoryRepository;
    @Autowired private GeometryFactory geometryFactory;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

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

        save("Sakura",        StoreType.RESTAURANT, asian,   4.8, 2, USER_LAT,         USER_LNG,            LocalTime.of(0, 0),  LocalTime.of(23, 59));
        save("Cheap Noodles", StoreType.RESTAURANT, asian,   2.5, 1, USER_LAT + 0.001, USER_LNG + 0.001,    LocalTime.of(0, 0),  LocalTime.of(23, 59));
        save("Roma",          StoreType.RESTAURANT, italian, 4.2, 3, USER_LAT,         USER_LNG + 0.0005,   LocalTime.of(0, 0),  LocalTime.of(23, 59));
        save("Far Pizza",     StoreType.RESTAURANT, italian, 4.0, 2, USER_LAT + 0.05,  USER_LNG + 0.10,     LocalTime.of(0, 0),  LocalTime.of(23, 59));
        save("Closed Sushi",  StoreType.BAKERY,     asian,   4.9, 4, USER_LAT,         USER_LNG,            LocalTime.of(3, 0),  LocalTime.of(3, 1));
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
            assertThat(node.path("categoryName").asText()).isEqualTo("Italian");
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

        assertThat(content).hasSize(1);
        JsonNode sample = content.get(0);
        assertThat(sample.path("type").asText()).isEqualTo("RESTAURANT");
        assertThat(sample.path("category").path("slug").asText()).isEqualTo("asian");
        assertThat(sample.path("rating").decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("4.0"));
    }

    @Test
    void filtersByPriceLevelMultiValued() throws Exception {
        JsonNode content = call("/stores?priceLevel=1&priceLevel=2").path("data").path("content");

        assertThat(content).hasSize(4);
        for (JsonNode node : content) {
            int level = node.path("priceLevel").asInt();
            assertThat(level).isIn(1, 2);
        }
    }

    @Test
    void filtersByMaxDistance() throws Exception {
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

        for (JsonNode node : content) {
            assertThat(node.path("name").asText()).isNotEqualTo("Closed Sushi");
        }
    }

    @Test
    void nearbyBranchReturnsRealData_notNull() throws Exception {
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

    @Test
    void storeDto_deliveryFee_serializes_as_amount_plus_currency_not_amountMinor() throws Exception {
        CategoryEntity cat = categoryRepository.save(CategoryEntity.builder()
                .name("Fee Test").slug("fee-test").iconName("fee").build());
        saveWithFee("FeeStore", StoreType.RESTAURANT, cat, 4.0, 2,
                USER_LAT, USER_LNG, LocalTime.of(0, 0), LocalTime.of(23, 59),
                Money.ofMajor(new BigDecimal("3.50"), Currency.getInstance("UAH")));

        JsonNode content = call("/stores?search=FeeStore").path("data").path("content");
        assertThat(content).hasSize(1);

        JsonNode deliveryFee = content.get(0).path("deliveryFee");
        assertThat(deliveryFee.isObject())
                .as("deliveryFee must be a JSON object {amount, currency}")
                .isTrue();
        assertThat(deliveryFee.path("amount").asText()).isEqualTo("3.50");
        assertThat(deliveryFee.path("currency").asText()).isEqualTo("UAH");
        assertThat(deliveryFee.has("amountMinor"))
                .as("amountMinor must not appear in wire form")
                .isFalse();
    }

    // --- HTTP QUERY method: same search, filters carried in a JSON body ---

    @Test
    void queryMethod_filtersByMinRating_sameContractAsGet() throws Exception {
        JsonNode content = query("""
                { "minRating": 4.5 }
                """).path("data").path("content");

        assertThat(content.size()).isGreaterThanOrEqualTo(2);
        for (JsonNode node : content) {
            assertThat(node.path("rating").decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("4.5"));
        }
    }

    @Test
    void queryMethod_geoFilterInBody_excludesFarStore() throws Exception {
        JsonNode content = query("""
                { "within": { "lat": %s, "lng": %s, "radiusKm": 1 } }
                """.formatted(USER_LAT, USER_LNG)).path("data").path("content");

        assertThat(content).hasSize(5);
        for (JsonNode node : content) {
            assertThat(node.path("name").asText()).isNotEqualTo("Far Pizza");
        }
    }

    @Test
    void queryMethod_invalidSortInBody_returns400_withEnvelope() throws Exception {
        HttpResponse<String> response = sendQuery("""
                { "sort": "password" }
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asText()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void queryMethod_malformedBody_returns400_withSameEnvelope() throws Exception {
        HttpResponse<String> response = sendQuery("{ not json");

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode body = objectMapper.readTree(response.body());
        // Same ApiResponse envelope as an invalid sort — not the ErrorDetails shape.
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asText()).isEqualTo("BAD_REQUEST");
    }

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

    private JsonNode query(String jsonBody) throws Exception {
        HttpResponse<String> response = sendQuery(jsonBody);
        assertThat(response.statusCode())
                .as("QUERY status, body=%s", response.body())
                .isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> sendQuery(String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/stores"))
                .header("X-Internal-Token", "test-internal-secret")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .method("QUERY", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
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
