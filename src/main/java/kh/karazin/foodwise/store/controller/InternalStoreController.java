package kh.karazin.foodwise.store.controller;

import kh.karazin.foodwise.common.dto.internal.InternalMenuItemDto;
import kh.karazin.foodwise.common.dto.internal.InternalStoreDto;
import kh.karazin.foodwise.store.service.StoreMenuItemService;
import kh.karazin.foodwise.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal endpoints for inter-service communication (not exposed via gateway).
 *
 * <p>Returns typed {@code Internal*Dto} records per ADR 0010 — no
 * {@code ApiResponse} envelope, no public-DTO leak (menu items, combos,
 * opening hours, etc. are intentionally absent).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalStoreController {

    private final StoreService storeService;
    private final StoreMenuItemService menuItemService;

    @GetMapping("/stores/{storeId}")
    public InternalStoreDto getStore(@PathVariable UUID storeId) {
        return storeService.getStoreForInternal(storeId);
    }

    @GetMapping("/menu-items/{itemId}")
    public InternalMenuItemDto getMenuItem(@PathVariable UUID itemId) {
        return menuItemService.getMenuItemForInternal(itemId);
    }

    /**
     * Unified item lookup: resolves {@code itemId} against menu_items first,
     * then surprise_boxes. Returns the same {@link InternalMenuItemDto} shape
     * for full backward-compatibility with cart-service.
     */
    @GetMapping("/items/{itemId}")
    public InternalMenuItemDto getItem(@PathVariable UUID itemId) {
        return menuItemService.findItemById(itemId);
    }
}
