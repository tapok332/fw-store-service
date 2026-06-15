package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.store.adapter.out.persistence.CategoryEntity;
import kh.karazin.foodwise.store.adapter.out.persistence.CategoryJpaRepository;
import kh.karazin.foodwise.store.domain.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for /home/categories (HomeController) and /categories/{slug} (CategoryController).
 *
 * <p>Verifies locale resolution, group filtering, mutual-exclusion validation
 * and per-slug lookup against a real PostGIS container.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "internal.service.secret=test-internal-secret",
        "foodwise.store.timezone=Europe/Kyiv",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class CategoryControllerIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4-alpine")
                    .asCompatibleSubstituteFor("postgres"))
            .withReuse(true);

    @LocalServerPort
    private int port;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private JsonMapper objectMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void seedTwoCategories() {
        categoryRepository.deleteAll();

        categoryRepository.save(CategoryEntity.builder()
                .slug("pizza")
                .name("Pizza")
                .iconName("pizza")
                .translations(new HashMap<>(Map.of("en", "Pizza", "uk", "Піца")))
                .applicableTypes(new HashSet<>(Set.of(StoreType.RESTAURANT)))
                .build());

        categoryRepository.save(CategoryEntity.builder()
                .slug("produce")
                .name("Produce")
                .iconName("apple")
                .translations(new HashMap<>(Map.of("en", "Produce", "uk", "Овочі та фрукти")))
                .applicableTypes(new HashSet<>(Set.of(StoreType.GROCERY)))
                .build());
    }

    @Test
    void getCategories_filtersByGroupFoodService() throws Exception {
        JsonNode body = get("/home/categories?group=FOOD_SERVICE", null);

        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("data")).hasSize(1);
        assertThat(body.get("data").get(0).get("slug").asText()).isEqualTo("pizza");
    }

    @Test
    void getCategories_filtersByGroupRetail() throws Exception {
        JsonNode body = get("/home/categories?group=RETAIL", null);

        assertThat(body.get("data")).hasSize(1);
        assertThat(body.get("data").get(0).get("slug").asText()).isEqualTo("produce");
    }

    @Test
    void getCategories_returnsUkrainianName_withAcceptLanguageUk() throws Exception {
        JsonNode body = get("/home/categories?group=RETAIL", "uk-UA");

        assertThat(body.get("data").get(0).get("name").asText())
                .isEqualTo("Овочі та фрукти");
    }

    @Test
    void getCategories_localeQueryOverridesHeader() throws Exception {
        JsonNode body = get("/home/categories?group=RETAIL&locale=uk", "en");

        assertThat(body.get("data").get(0).get("name").asText())
                .isEqualTo("Овочі та фрукти");
    }

    @Test
    void getCategories_returnsBadRequest_onBothGroupAndType() throws Exception {
        int s = status("/home/categories?group=RETAIL&type=GROCERY", null);
        assertThat(s).isEqualTo(400);
    }

    @Test
    void getCategories_returnsBadRequest_onUnsupportedLocale() throws Exception {
        int s = status("/home/categories?locale=fr", null);
        assertThat(s).isEqualTo(400);
    }

    @Test
    void getCategoryBySlug_returnsLocalizedName() throws Exception {
        JsonNode body = get("/categories/pizza", "uk");

        assertThat(body.get("data").get("name").asText()).isEqualTo("Піца");
        assertThat(body.get("data").get("applicableTypes").get(0).asText())
                .isEqualTo("RESTAURANT");
        assertThat(body.get("data").get("applicableGroups").get(0).asText())
                .isEqualTo("FOOD_SERVICE");
    }

    private JsonNode get(String path, String acceptLanguage) throws Exception {
        HttpRequest req = buildRequest(path, acceptLanguage);
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode())
                .as("status for %s, body=%s", path, res.body())
                .isBetween(200, 299);
        return objectMapper.readTree(res.body());
    }

    private int status(String path, String acceptLanguage) throws Exception {
        HttpRequest req = buildRequest(path, acceptLanguage);
        return http.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private HttpRequest buildRequest(String path, String acceptLanguage) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://localhost:" + port + path))
                .header("X-Internal-Token", "test-internal-secret")
                .timeout(Duration.ofSeconds(10));
        if (acceptLanguage != null) {
            b.header("Accept-Language", acceptLanguage);
        }
        return b.build();
    }
}
