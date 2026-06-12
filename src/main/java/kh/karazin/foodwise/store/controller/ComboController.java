package kh.karazin.foodwise.store.controller;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.dto.ComboCreateRequest;
import kh.karazin.foodwise.store.dto.ComboDto;
import kh.karazin.foodwise.store.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin/seed endpoint for creating store combos.
 */
@RestController
@RequestMapping("/stores/{storeId}/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ComboDto> createCombo(
            @PathVariable UUID storeId,
            @Valid @RequestBody ComboCreateRequest request) {
        return ApiResponse.success(comboService.createCombo(storeId, request));
    }
}
