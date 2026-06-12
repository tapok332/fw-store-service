package kh.karazin.foodwise.store.controller;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.dto.StoreMenuItemDto;
import kh.karazin.foodwise.store.service.StoreMenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller for store menu item CRUD operations.
 */
@RestController
@RequestMapping("/stores/{storeId}/menu-items")
@RequiredArgsConstructor
public class StoreMenuItemController {

    private final StoreMenuItemService menuItemService;

    /**
     * Get paginated menu items for a store.
     */
    @GetMapping
    public ApiResponse<Page<StoreMenuItemDto>> getMenuItems(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(menuItemService.getMenuItems(storeId, page, size));
    }

    /**
     * Create a new menu item.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StoreMenuItemDto> createMenuItem(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreMenuItemDto dto) {
        StoreMenuItemDto created = menuItemService.createMenuItem(
                new StoreMenuItemDto(null, dto.name(), dto.description(), dto.price(),
                        dto.imageUrl(), dto.legacyCategory(), dto.available(),
                        storeId, dto.sectionId()));
        return ApiResponse.success(created);
    }

    /**
     * Update a menu item.
     */
    @PutMapping("/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StoreMenuItemDto> updateMenuItem(
            @PathVariable UUID storeId,
            @PathVariable UUID itemId,
            @Valid @RequestBody StoreMenuItemDto dto) {
        return ApiResponse.success(menuItemService.updateMenuItem(itemId, dto));
    }

    /**
     * Delete a menu item.
     */
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMenuItem(
            @PathVariable UUID storeId,
            @PathVariable UUID itemId) {
        menuItemService.deleteMenuItem(itemId);
    }
}
