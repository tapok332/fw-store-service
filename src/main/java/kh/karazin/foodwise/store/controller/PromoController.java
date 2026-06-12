package kh.karazin.foodwise.store.controller;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.dto.PromoAdminDto;
import kh.karazin.foodwise.store.service.PromoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD controller for store promotions.
 */
@RestController
@RequestMapping("/promos")
@RequiredArgsConstructor
public class PromoController {

    private final PromoService promoService;

    /**
     * Get all promos for a store (admin).
     */
    @GetMapping("/store/{storeId}")
    public ApiResponse<List<PromoAdminDto>> getPromosByStore(@PathVariable UUID storeId) {
        return ApiResponse.success(promoService.getAllPromosByStoreId(storeId));
    }

    /**
     * Create a new promo.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PromoAdminDto> createPromo(@Valid @RequestBody PromoAdminDto dto) {
        return ApiResponse.success(promoService.createPromo(dto));
    }

    /**
     * Update a promo.
     */
    @PutMapping("/{promoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PromoAdminDto> updatePromo(
            @PathVariable UUID promoId,
            @Valid @RequestBody PromoAdminDto dto) {
        return ApiResponse.success(promoService.updatePromo(promoId, dto));
    }

    /**
     * Delete a promo.
     */
    @DeleteMapping("/{promoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePromo(@PathVariable UUID promoId) {
        promoService.deletePromo(promoId);
    }
}
