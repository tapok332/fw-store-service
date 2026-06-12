package kh.karazin.foodwise.store.mapper;

import kh.karazin.foodwise.common.dto.internal.InternalLocationDto;
import kh.karazin.foodwise.common.dto.internal.InternalMenuItemDto;
import kh.karazin.foodwise.common.dto.internal.InternalStoreDto;
import kh.karazin.foodwise.store.config.StoreProperties;
import kh.karazin.foodwise.store.dto.CategoryDto;
import kh.karazin.foodwise.store.dto.ComboDto;
import kh.karazin.foodwise.store.dto.LocationDto;
import kh.karazin.foodwise.store.dto.MenuItemDto;
import kh.karazin.foodwise.store.dto.StoreDto;
import kh.karazin.foodwise.store.dto.StoreMenuItemDto;
import kh.karazin.foodwise.store.entity.CategoryEntity;
import kh.karazin.foodwise.store.entity.ComboEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.entity.StoreGroup;
import kh.karazin.foodwise.store.entity.StoreMenuItemEntity;
import kh.karazin.foodwise.store.entity.StoreType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Mapper for converting store entities to DTOs.
 */
@Component
@RequiredArgsConstructor
public class StoreMapper {

    private final StoreProperties properties;

    public StoreDto toDto(StoreEntity entity) {
        return toDto(entity, null);
    }

    public StoreDto toDto(StoreEntity entity, Double distance) {
        LocationDto location = null;
        if (entity.getLocation() != null) {
            location = new LocationDto(
                    entity.getLocation().getY(),
                    entity.getLocation().getX()
            );
        }

        CategoryDto category = toCategoryDto(entity.getCategory());

        return new StoreDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                category,
                entity.getDescription(),
                entity.getImageUrl(),
                entity.getHeroImageUrl(),
                entity.getAddress(),
                location,
                entity.getRating(),
                entity.getOpensAt(),
                entity.getClosesAt(),
                entity.getPhone(),
                entity.getWebsite(),
                entity.getDeliveryFee(),
                entity.getMinOrderAmount(),
                entity.getPriceLevel(),
                entity.isCurrentlyOpen(properties.timezone()),
                Collections.emptyList(),
                Collections.emptyList(),
                distance
        );
    }

    public StoreDto toDtoWithMenu(StoreEntity entity, List<StoreMenuItemEntity> menuItems,
                                   List<ComboEntity> combos, Double distance) {
        LocationDto location = null;
        if (entity.getLocation() != null) {
            location = new LocationDto(
                    entity.getLocation().getY(),
                    entity.getLocation().getX()
            );
        }

        CategoryDto category = toCategoryDto(entity.getCategory());

        List<MenuItemDto> menuItemDtos = menuItems != null
                ? menuItems.stream().map(this::toMenuItemDto).toList()
                : Collections.emptyList();

        List<ComboDto> comboDtos = combos != null
                ? combos.stream().map(this::toComboDto).toList()
                : Collections.emptyList();

        return new StoreDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                category,
                entity.getDescription(),
                entity.getImageUrl(),
                entity.getHeroImageUrl(),
                entity.getAddress(),
                location,
                entity.getRating(),
                entity.getOpensAt(),
                entity.getClosesAt(),
                entity.getPhone(),
                entity.getWebsite(),
                entity.getDeliveryFee(),
                entity.getMinOrderAmount(),
                entity.getPriceLevel(),
                entity.isCurrentlyOpen(properties.timezone()),
                menuItemDtos,
                comboDtos,
                distance
        );
    }

    public MenuItemDto toMenuItemDto(StoreMenuItemEntity entity) {
        return new MenuItemDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getImageUrl(),
                entity.getAvailable()
        );
    }

    public StoreMenuItemDto toStoreMenuItemDto(StoreMenuItemEntity entity) {
        return new StoreMenuItemDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getImageUrl(),
                entity.getLegacyCategory(),
                entity.getAvailable(),
                entity.getStore().getId(),
                entity.getSection() != null ? entity.getSection().getId() : null
        );
    }

    public CategoryDto toCategoryDto(CategoryEntity entity) {
        return toCategoryDto(entity, Locale.ENGLISH);
    }

    public CategoryDto toCategoryDto(CategoryEntity entity, Locale locale) {
        if (entity == null) return null;
        String name = resolveName(entity, locale);
        List<StoreType> types = entity.getApplicableTypes().stream().sorted().toList();
        List<StoreGroup> groups = entity.getApplicableTypes().stream()
                .map(StoreType::group).distinct().sorted().toList();
        return new CategoryDto(entity.getId(), entity.getSlug(), name,
                entity.getIconName(), types, groups);
    }

    private static String resolveName(CategoryEntity entity, Locale locale) {
        String key = locale != null ? locale.getLanguage() : "en";
        String hit = entity.getTranslations().get(key);
        if (hit != null) return hit;
        String en = entity.getTranslations().get("en");
        if (en != null) return en;
        return entity.getName();
    }

    /**
     * Maps a store entity to its typed internal wire DTO per ADR 0010.
     * Excludes menu items, combos, distance, opening hours, and rating —
     * downstream consumers (order-service, surprisebox-service) read only
     * {@code id}, {@code name}, {@code location}, {@code deliveryFee},
     * {@code minOrderAmount}, {@code imageUrl}.
     */
    public InternalStoreDto toInternalDto(StoreEntity entity) {
        InternalLocationDto location = null;
        if (entity.getLocation() != null) {
            location = new InternalLocationDto(
                    entity.getLocation().getY(),
                    entity.getLocation().getX()
            );
        }
        return new InternalStoreDto(
                entity.getId(),
                entity.getName(),
                entity.getImageUrl(),
                location,
                entity.getDeliveryFee(),
                entity.getMinOrderAmount()
        );
    }

    /**
     * Maps a menu-item entity to its typed internal wire DTO per ADR 0010.
     * Excludes {@code description}, {@code legacyCategory}, {@code sectionId}.
     */
    public InternalMenuItemDto toInternalMenuItemDto(StoreMenuItemEntity entity) {
        return new InternalMenuItemDto(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getImageUrl(),
                entity.getStore().getId(),
                entity.getAvailable()
        );
    }

    public ComboDto toComboDto(ComboEntity entity) {
        List<MenuItemDto> items = entity.getMenuItems() != null
                ? entity.getMenuItems().stream().map(this::toMenuItemDto).toList()
                : Collections.emptyList();

        return new ComboDto(
                entity.getId(),
                entity.getTitle(),
                entity.getPrice(),
                entity.getImageUrl(),
                entity.getSavings(),
                items
        );
    }
}
