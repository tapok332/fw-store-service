package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.application.port.in.ComboUseCase;
import kh.karazin.foodwise.store.application.port.in.CreateComboCommand;
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

    private final ComboUseCase comboUseCase;
    private final StoreRestMapper storeRestMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ComboDto> createCombo(
            @PathVariable UUID storeId,
            @Valid @RequestBody ComboCreateRequest request) {
        CreateComboCommand command = new CreateComboCommand(
                storeId, request.title(), request.price(), request.imageUrl(),
                request.savings(), request.menuItemIds());
        return ApiResponse.success(storeRestMapper.toComboDto(comboUseCase.createCombo(command)));
    }
}
