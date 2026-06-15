package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.application.port.in.MenuItemUseCase;
import kh.karazin.foodwise.store.application.port.in.UpdateMenuItemCommand;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.MenuSectionId;
import kh.karazin.foodwise.store.domain.StoreId;
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

    private final MenuItemUseCase menuItemUseCase;
    private final StoreRestMapper storeRestMapper;

    /**
     * Get paginated menu items for a store.
     */
    @GetMapping
    public ApiResponse<Page<StoreMenuItemDto>> getMenuItems(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<StoreMenuItemDto> result = menuItemUseCase.getMenuItems(new StoreId(storeId), page, size)
                .map(storeRestMapper::toStoreMenuItemDto);
        return ApiResponse.success(result);
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
        MenuItem menuItem = MenuItem.create(
                dto.name(), dto.description(), dto.price(), dto.imageUrl(), dto.legacyCategory(),
                dto.available(), new StoreId(storeId),
                dto.sectionId() != null ? new MenuSectionId(dto.sectionId()) : null);
        return ApiResponse.success(storeRestMapper.toStoreMenuItemDto(
                menuItemUseCase.createMenuItem(menuItem)));
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
        UpdateMenuItemCommand command = new UpdateMenuItemCommand(
                dto.name(), dto.description(), dto.price(), dto.imageUrl(),
                dto.legacyCategory(), dto.available());
        return ApiResponse.success(storeRestMapper.toStoreMenuItemDto(
                menuItemUseCase.updateMenuItem(new MenuItemId(itemId), command)));
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
        menuItemUseCase.deleteMenuItem(new MenuItemId(itemId));
    }
}
