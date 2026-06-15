package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.common.dto.internal.InternalLocationDto;
import kh.karazin.foodwise.common.dto.internal.InternalMenuItemDto;
import kh.karazin.foodwise.common.dto.internal.InternalStoreDto;
import kh.karazin.foodwise.store.config.StoreProperties;
import kh.karazin.foodwise.store.domain.CatalogItem;
import kh.karazin.foodwise.store.domain.Combo;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuSearchResult;
import kh.karazin.foodwise.store.domain.MenuSection;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreReview;
import kh.karazin.foodwise.store.domain.StoreWithMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Maps store-family domain objects to their public wire DTOs. The embedded
 * category is localized to English to match the prior contract; the store-detail
 * variant additionally carries the available menu items and combos.
 */
@Component
@RequiredArgsConstructor
class StoreRestMapper {

    private final StoreProperties properties;
    private final CategoryRestMapper categoryRestMapper;

    StoreDto toDto(Store store) {
        return new StoreDto(
                store.id().value(),
                store.name(),
                store.type(),
                categoryRestMapper.toDto(store.category(), Locale.ENGLISH),
                store.description(),
                store.imageUrl(),
                store.heroImageUrl(),
                store.address(),
                toLocation(store),
                store.rating(),
                store.opensAt(),
                store.closesAt(),
                store.phone(),
                store.website(),
                store.deliveryFee(),
                store.minOrderAmount(),
                store.priceLevel(),
                store.isCurrentlyOpen(properties.timezone()),
                Collections.emptyList(),
                Collections.emptyList(),
                null);
    }

    StoreDto toDtoWithMenu(StoreWithMenu storeWithMenu) {
        Store store = storeWithMenu.store();
        List<MenuItemDto> menuItemDtos = storeWithMenu.menuItems() != null
                ? storeWithMenu.menuItems().stream().map(this::toMenuItemDto).toList()
                : Collections.emptyList();
        List<ComboDto> comboDtos = storeWithMenu.combos() != null
                ? storeWithMenu.combos().stream().map(this::toComboDto).toList()
                : Collections.emptyList();

        return new StoreDto(
                store.id().value(),
                store.name(),
                store.type(),
                categoryRestMapper.toDto(store.category(), Locale.ENGLISH),
                store.description(),
                store.imageUrl(),
                store.heroImageUrl(),
                store.address(),
                toLocation(store),
                store.rating(),
                store.opensAt(),
                store.closesAt(),
                store.phone(),
                store.website(),
                store.deliveryFee(),
                store.minOrderAmount(),
                store.priceLevel(),
                store.isCurrentlyOpen(properties.timezone()),
                menuItemDtos,
                comboDtos,
                storeWithMenu.distance());
    }

    MenuItemDto toMenuItemDto(MenuItem item) {
        return new MenuItemDto(
                item.id().value(),
                item.name(),
                item.description(),
                item.price(),
                item.imageUrl(),
                item.available());
    }

    StoreMenuItemDto toStoreMenuItemDto(MenuItem item) {
        return new StoreMenuItemDto(
                item.id().value(),
                item.name(),
                item.description(),
                item.price(),
                item.imageUrl(),
                item.legacyCategory(),
                item.available(),
                item.storeId().value(),
                item.sectionId() != null ? item.sectionId().value() : null);
    }

    ComboDto toComboDto(Combo combo) {
        List<MenuItemDto> items = combo.items() != null
                ? combo.items().stream().map(this::toMenuItemDto).toList()
                : Collections.emptyList();
        return new ComboDto(
                combo.id().value(),
                combo.title(),
                combo.price(),
                combo.imageUrl(),
                combo.savings(),
                items);
    }

    MenuSearchResponse toMenuSearchResponse(MenuSearchResult result) {
        List<StoreMenuItemDto> dtos = result.items().stream().map(this::toStoreMenuItemDto).toList();
        return new MenuSearchResponse(result.query(), dtos.size(), dtos);
    }

    MenuSectionDto toMenuSectionDto(MenuSection section) {
        return new MenuSectionDto(
                section.id().value(),
                section.storeId().value(),
                section.title(),
                section.sortOrder());
    }

    ReviewDto toReviewDto(StoreReview review) {
        return new ReviewDto(
                review.id().value(),
                review.storeId().value(),
                review.profileId().value(),
                review.orderId(),
                review.rating(),
                review.comment(),
                review.createdAt());
    }

    /**
     * Maps a store to its typed internal wire DTO per ADR 0010. Excludes menu
     * items, combos, distance, opening hours, and rating — downstream consumers
     * read only {@code id}, {@code name}, {@code location}, {@code deliveryFee},
     * {@code minOrderAmount}, {@code imageUrl}.
     */
    InternalStoreDto toInternalDto(Store store) {
        InternalLocationDto location = (store.latitude() != null && store.longitude() != null)
                ? new InternalLocationDto(store.latitude(), store.longitude())
                : null;
        return new InternalStoreDto(
                store.id().value(),
                store.name(),
                store.imageUrl(),
                location,
                store.deliveryFee(),
                store.minOrderAmount());
    }

    /**
     * Maps a unified catalog item to the internal menu-item wire DTO per ADR 0010.
     */
    InternalMenuItemDto toInternalMenuItemDto(CatalogItem item) {
        return new InternalMenuItemDto(
                item.id(),
                item.name(),
                item.price(),
                item.imageUrl(),
                item.storeId(),
                item.available());
    }

    private LocationDto toLocation(Store store) {
        if (store.latitude() == null || store.longitude() == null) {
            return null;
        }
        return new LocationDto(store.latitude(), store.longitude());
    }
}
