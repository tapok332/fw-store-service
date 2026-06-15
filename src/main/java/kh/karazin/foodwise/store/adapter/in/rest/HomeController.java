package kh.karazin.foodwise.store.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.application.port.in.CategoryUseCase;
import kh.karazin.foodwise.store.application.port.in.ContentUseCase;
import kh.karazin.foodwise.store.application.port.in.StoreUseCase;
import kh.karazin.foodwise.store.application.port.in.SurpriseBoxQueryUseCase;
import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Home screen controller — aggregates stores, boxes, categories, and content.
 */
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "Homepage content and discovery")
public class HomeController {

    private final StoreUseCase storeUseCase;
    private final CategoryUseCase categoryUseCase;
    private final ContentUseCase contentUseCase;
    private final SurpriseBoxQueryUseCase surpriseBoxQueryUseCase;
    private final RequestLocaleResolver localeResolver;
    private final StoreRestMapper storeRestMapper;
    private final CategoryRestMapper categoryRestMapper;
    private final SurpriseBoxRestMapper surpriseBoxRestMapper;
    private final ContentRestMapper contentRestMapper;

    @Operation(summary = "Get featured stores")
    @GetMapping("/featured-stores")
    public ResponseEntity<ApiResponse<List<StoreDto>>> getFeaturedStores() {
        List<StoreDto> stores = storeUseCase.getFeaturedStores().stream()
                .map(storeRestMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @Operation(summary = "Get stores near location")
    @GetMapping("/stores/nearby")
    public ResponseEntity<ApiResponse<List<StoreDto>>> getNearbyStores(
            @Parameter(description = "Latitude") @RequestParam Double lat,
            @Parameter(description = "Longitude") @RequestParam Double lng,
            @Parameter(description = "Radius in km") @RequestParam(defaultValue = "5") Double radius) {
        List<StoreDto> stores = storeUseCase.findNearbyStores(lat, lng, radius).stream()
                .map(storeRestMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @Operation(summary = "Get surprise boxes for homepage")
    @GetMapping("/boxes")
    public ResponseEntity<ApiResponse<List<SurpriseBoxHomeDto>>> getBoxes(
            @RequestParam(defaultValue = "50.45") Double lat,
            @RequestParam(defaultValue = "30.52") Double lng,
            @RequestParam(defaultValue = "100000") Double radius) {
        List<SurpriseBoxHomeDto> dtos = surpriseBoxQueryUseCase.findNearbyForHome(lat, lng, radius).stream()
                .map(surpriseBoxRestMapper::toHomeDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get all categories")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories(
            @RequestParam(required = false) StoreGroup group,
            @RequestParam(name = "type", required = false) List<StoreType> types,
            HttpServletRequest req) {
        Set<StoreType> typeSet = types != null ? new HashSet<>(types) : Set.of();
        Locale locale = localeResolver.resolve(req);
        List<CategoryDto> result = categoryUseCase.findAll(group, typeSet).stream()
                .map(c -> categoryRestMapper.toDto(c, locale))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Get hero banner images")
    @GetMapping("/hero-images")
    public ResponseEntity<ApiResponse<List<HeroImageDto>>> getHeroImages() {
        List<HeroImageDto> dtos = contentUseCase.getHeroImages().stream()
                .map(contentRestMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Get category icons")
    @GetMapping("/category-icons")
    public ResponseEntity<ApiResponse<List<CategoryIconDto>>> getCategoryIcons() {
        List<CategoryIconDto> dtos = contentUseCase.getCategoryIcons().stream()
                .map(contentRestMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}
