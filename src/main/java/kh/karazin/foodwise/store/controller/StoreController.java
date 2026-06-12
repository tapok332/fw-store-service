package kh.karazin.foodwise.store.controller;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.dto.MenuSearchResponse;
import kh.karazin.foodwise.store.dto.PromoDto;
import kh.karazin.foodwise.store.dto.StoreCreateRequest;
import kh.karazin.foodwise.store.dto.StoreDto;
import kh.karazin.foodwise.store.dto.StoreSearchParams;
import kh.karazin.foodwise.store.entity.StoreGroup;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.service.PromoService;
import kh.karazin.foodwise.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public store endpoints.
 */
@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final PromoService promoService;

    /**
     * Search stores with filters.
     *
     * @param search        free-text search across name + description
     * @param type          macro-classification ({@link StoreType}); mutually exclusive with {@code group}
     * @param group         macro-group ({@link StoreGroup}); mutually exclusive with {@code type}
     * @param categoryId    cuisine category UUID (prefer over slug if both given)
     * @param categorySlug  cuisine category slug (lower-kebab-case)
     * @param latitude      user latitude (decimal degrees, WGS84)
     * @param longitude     user longitude
     * @param minRating     minimum rating (inclusive)
     * @param maxDistance   maximum distance from user in kilometers (requires lat+lng)
     * @param openNow       only stores currently open in the store timezone
     * @param priceLevel    one or more price tiers; multi-valued (?priceLevel=1&amp;priceLevel=2)
     * @param sort          one of: distance, rating, priceAsc, priceDesc (direction suffix accepted)
     * @param page          0-based page index
     * @param limit         page size (capped at 100)
     */
    @GetMapping
    public ApiResponse<Page<StoreDto>> getStores(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StoreType type,
            @RequestParam(required = false) StoreGroup group,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Boolean openNow,
            @RequestParam(required = false) List<Integer> priceLevel,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {

        StoreSearchParams params = new StoreSearchParams(
                search, type, group, categoryId, categorySlug, latitude, longitude,
                minRating, maxDistance, openNow, priceLevel,
                StoreSearchParams.SortField.parse(sort), page, limit);

        return ApiResponse.success(storeService.searchStores(params));
    }

    /**
     * Get store by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<StoreDto> getStore(
            @PathVariable UUID id,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        StoreDto store;
        if (latitude != null && longitude != null) {
            store = storeService.getStoreByIdWithDistance(id, latitude, longitude);
        } else {
            store = storeService.getStoreById(id);
        }
        return ApiResponse.success(store);
    }

    /**
     * Get active promos for a store.
     */
    @GetMapping("/{storeId}/promos")
    public ApiResponse<List<PromoDto>> getStorePromos(@PathVariable UUID storeId) {
        List<PromoDto> promos = promoService.getActivePromosByStoreId(storeId);
        return ApiResponse.success(promos);
    }

    /**
     * Search menu items within a store.
     */
    @GetMapping("/{storeId}/menu/search")
    public ApiResponse<MenuSearchResponse> searchMenu(
            @PathVariable UUID storeId,
            @RequestParam String query) {
        MenuSearchResponse response = storeService.searchMenu(storeId, query);
        return ApiResponse.success(response);
    }

    /**
     * Create a new store (admin/seed).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StoreDto> createStore(@Valid @RequestBody StoreCreateRequest request) {
        return ApiResponse.success(storeService.createStore(request));
    }

    /**
     * Translate an unknown {@code sort} value (and other malformed query params)
     * into a 400 response instead of the default 500.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error("BAD_REQUEST", e.getMessage());
    }
}
