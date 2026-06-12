package kh.karazin.foodwise.store.client;

import kh.karazin.foodwise.common.dto.internal.InternalSurpriseBoxDto;
import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
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
 * HTTP client for fetching surprise-box data from surprisebox-service internal API.
 * Used by store-service's unified item lookup (GET /internal/items/{id}).
 *
 * <p>Handles both the no-envelope ADR 0010 format (direct DTO) and the legacy
 * {@code {"success":true,"data":{...}}} envelope produced by older deployed
 * instances — whichever is present in the actual response.
 *
 * <p>Uses a lenient ObjectMapper so unknown fields (e.g. {@code "lat"/"lng"} in
 * {@code location} from older deployments) are silently ignored. Only the fields
 * consumed by cart-service ({@code id}, {@code title}, {@code price},
 * {@code imageUrl}, {@code store.storeId}, {@code stock}) are required.
 */
@Slf4j
@Component
public class SurpriseBoxServiceClient {

    private final RestClient surpriseBoxRestClient;
    private final JsonMapper lenientMapper;

    public SurpriseBoxServiceClient(RestClient surpriseBoxRestClient, JsonMapper objectMapper) {
        this.surpriseBoxRestClient = surpriseBoxRestClient;
        // Reuse the auto-configured Jackson 3 mapper (carries MoneyJacksonModule via
        // MoneyJacksonConfig) so the Money-typed price deserializes; just relax unknown
        // fields for tolerance to older surprisebox-service payloads.
        this.lenientMapper = objectMapper.rebuild()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    /**
     * Returns the surprise box with the given {@code boxId}, or an empty
     * Optional if the box is not found (404). Other HTTP errors propagate
     * as {@link FoodWiseException} SERVICE_UNAVAILABLE.
     */
    public Optional<InternalSurpriseBoxDto> findById(UUID boxId) {
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
            return Optional.ofNullable(dto);
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
