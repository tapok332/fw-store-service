package kh.karazin.foodwise.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.dto.CategoryIconDto;
import kh.karazin.foodwise.store.dto.HeroImageDto;
import kh.karazin.foodwise.store.dto.StoreDto;
import kh.karazin.foodwise.store.dto.SurpriseBoxHomeDto;
import kh.karazin.foodwise.store.entity.StoreGroup;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.entity.SurpriseBoxEntity;
import kh.karazin.foodwise.store.i18n.RequestLocaleResolver;
import kh.karazin.foodwise.store.repository.SurpriseBoxRepository;
import kh.karazin.foodwise.store.service.CategoryService;
import kh.karazin.foodwise.store.service.ContentService;
import kh.karazin.foodwise.store.service.StoreService;
import kh.karazin.foodwise.store.dto.CategoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Home screen controller — aggregates stores, boxes, categories, and content.
 */
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "Homepage content and discovery")
public class HomeController {

    private final StoreService storeService;
    private final CategoryService categoryService;
    private final ContentService contentService;
    private final SurpriseBoxRepository surpriseBoxRepository;
    private final RequestLocaleResolver localeResolver;

    @Operation(summary = "Get featured stores")
    @GetMapping("/featured-stores")
    public ResponseEntity<ApiResponse<List<StoreDto>>> getFeaturedStores() {
        List<StoreDto> stores = storeService.getFeaturedStores();
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @Operation(summary = "Get stores near location")
    @GetMapping("/stores/nearby")
    public ResponseEntity<ApiResponse<List<StoreDto>>> getNearbyStores(
            @Parameter(description = "Latitude") @RequestParam Double lat,
            @Parameter(description = "Longitude") @RequestParam Double lng,
            @Parameter(description = "Radius in km") @RequestParam(defaultValue = "5") Double radius) {
        List<StoreDto> stores = storeService.findNearbyStores(lat, lng, radius);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @Operation(summary = "Get surprise boxes for homepage")
    @GetMapping("/boxes")
    public ResponseEntity<ApiResponse<List<SurpriseBoxHomeDto>>> getBoxes(
            @RequestParam(defaultValue = "50.45") Double lat,
            @RequestParam(defaultValue = "30.52") Double lng,
            @RequestParam(defaultValue = "100000") Double radius) {
        List<SurpriseBoxEntity> boxes = surpriseBoxRepository.findNearbyBoxesWithAvailableStock(lat, lng, radius);
        List<SurpriseBoxHomeDto> dtos = boxes.stream()
                .map(this::toHomeDto)
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
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.findAll(localeResolver.resolve(req), group, typeSet)));
    }

    @Operation(summary = "Get hero banner images")
    @GetMapping("/hero-images")
    public ResponseEntity<ApiResponse<List<HeroImageDto>>> getHeroImages() {
        return ResponseEntity.ok(ApiResponse.success(contentService.getHeroImages()));
    }

    @Operation(summary = "Get category icons")
    @GetMapping("/category-icons")
    public ResponseEntity<ApiResponse<List<CategoryIconDto>>> getCategoryIcons() {
        return ResponseEntity.ok(ApiResponse.success(contentService.getCategoryIcons()));
    }

    private SurpriseBoxHomeDto toHomeDto(SurpriseBoxEntity box) {
        var store = box.getStore();
        Double lat = null;
        Double lng = null;
        if (store.getLocation() != null) {
            lat = store.getLocation().getY();
            lng = store.getLocation().getX();
        }
        int discount = 0;
        if (box.getPrice() != null && box.getRetailPrice() != null && box.getRetailPrice().amountMinor() > 0) {
            long retail = box.getRetailPrice().amountMinor();
            long price  = box.getPrice().amountMinor();
            discount = Math.max(0, (int) Math.round((retail - price) * 100.0 / retail));
        }
        return new SurpriseBoxHomeDto(
                box.getId().toString(),
                box.getTitle(),
                box.getDescription(),
                box.getImageUrl(),
                box.getPrice(),
                box.getRetailPrice(),
                box.getStock(),
                box.getPickupFrom(),
                box.getPickupTo(),
                box.getDeliveryAvailable(),
                box.getCategory(),
                box.getRating() != null ? box.getRating().doubleValue() : null,
                discount,
                store.getId().toString(),
                store.getName(),
                store.getAddress(),
                lat,
                lng
        );
    }
}
