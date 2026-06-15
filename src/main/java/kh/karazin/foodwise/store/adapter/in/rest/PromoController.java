package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.application.port.in.CreatePromoCommand;
import kh.karazin.foodwise.store.application.port.in.PromoUseCase;
import kh.karazin.foodwise.store.application.port.in.UpdatePromoCommand;
import kh.karazin.foodwise.store.domain.PromoId;
import kh.karazin.foodwise.store.domain.StoreId;
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

    private final PromoUseCase promoUseCase;
    private final PromoRestMapper promoRestMapper;

    /**
     * Get all promos for a store (admin).
     */
    @GetMapping("/store/{storeId}")
    public ApiResponse<List<PromoAdminDto>> getPromosByStore(@PathVariable UUID storeId) {
        List<PromoAdminDto> promos = promoUseCase.getAllPromosByStoreId(new StoreId(storeId)).stream()
                .map(promoRestMapper::toAdminDto)
                .toList();
        return ApiResponse.success(promos);
    }

    /**
     * Create a new promo.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PromoAdminDto> createPromo(@Valid @RequestBody PromoAdminDto dto) {
        CreatePromoCommand command = new CreatePromoCommand(
                dto.storeId(), dto.title(), dto.description(), dto.emoji(),
                dto.bgColor(), dto.accentColor(), dto.active(), dto.priority());
        return ApiResponse.success(promoRestMapper.toAdminDto(promoUseCase.createPromo(command)));
    }

    /**
     * Update a promo.
     */
    @PutMapping("/{promoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PromoAdminDto> updatePromo(
            @PathVariable UUID promoId,
            @Valid @RequestBody PromoAdminDto dto) {
        UpdatePromoCommand command = new UpdatePromoCommand(
                dto.title(), dto.description(), dto.emoji(),
                dto.bgColor(), dto.accentColor(), dto.active(), dto.priority());
        return ApiResponse.success(promoRestMapper.toAdminDto(
                promoUseCase.updatePromo(new PromoId(promoId), command)));
    }

    /**
     * Delete a promo.
     */
    @DeleteMapping("/{promoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePromo(@PathVariable UUID promoId) {
        promoUseCase.deletePromo(new PromoId(promoId));
    }
}
