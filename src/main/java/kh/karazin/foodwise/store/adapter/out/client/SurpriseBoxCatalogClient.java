package kh.karazin.foodwise.store.adapter.out.client;

import kh.karazin.foodwise.common.dto.internal.InternalSurpriseBoxDto;
import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.out.SurpriseBoxCatalogGateway;
import kh.karazin.foodwise.store.domain.CatalogItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound adapter to surprisebox-service for the unified internal item lookup
 * ({@code GET /internal/items/{id}}). Implements {@link SurpriseBoxCatalogGateway},
 * mapping the surprise-box wire DTO onto the unified {@link CatalogItem} shape.
 *
 * <p>Handles both the no-envelope ADR 0010 format (direct DTO) and the legacy
 * {@code {"success":true,"data":{...}}} envelope produced by older deployed
 * instances — whichever is present in the actual response.
 *
 * <p>Uses a lenient ObjectMapper so unknown fields (e.g. {@code "lat"/"lng"} in
 * {@code location} from older deployments) are silently ignored.
 */
@Slf4j
@Component
class SurpriseBoxCatalogClient implements SurpriseBoxCatalogGateway {

    private final RestClient surpriseBoxRestClient;
    private final JsonMapper lenientMapper;

    SurpriseBoxCatalogClient(RestClient surpriseBoxRestClient, JsonMapper objectMapper) {
        this.surpriseBoxRestClient = surpriseBoxRestClient;
        // Reuse the auto-configured Jackson 3 mapper (carries MoneyJacksonModule via
        // MoneyJacksonConfig) so the Money-typed price deserializes; just relax unknown
        // fields for tolerance to older surprisebox-service payloads.
        this.lenientMapper = objectMapper.rebuild()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /**
     * Returns the surprise box with the given {@code boxId} as a unified catalog
     * item, or an empty Optional if not found (404). Other HTTP errors propagate
     * as {@link FoodWiseException} SERVICE_UNAVAILABLE.
     */
    @Override
    public Optional<CatalogItem> findBoxAsItem(UUID boxId) {
        try {
            // Read as String first — avoids the RestClient type-resolution issue
            // with JsonNode and handles both no-envelope (ADR 0010) and legacy
            // {"success":true,"data":{...}} envelope formats from older deployments.
            String body = surpriseBoxRestClient.get()
                    .uri("/internal/surprise-boxes/{boxId}", boxId)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = lenientMapper.readTree(body);
            // Handle legacy envelope {"success":true,"data":{...}}
            JsonNode payload = root.has("data") ? root.get("data") : root;

            InternalSurpriseBoxDto dto = lenientMapper.treeToValue(payload, InternalSurpriseBoxDto.class);
            if (dto == null) {
                return Optional.empty();
            }

            return Optional.of(new CatalogItem(
                    dto.id(),
                    dto.title(),
                    dto.price(),
                    dto.imageUrl(),
                    dto.store() != null ? dto.store().storeId() : null,
                    dto.stock() != null && dto.stock() > 0));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch surprise box {} from surprisebox-service: {}", boxId, e.getMessage());
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.SERVICE_UNAVAILABLE,
                    "Surprise-box service unavailable while looking up item: " + boxId);
        }
    }
}
