package kh.karazin.foodwise.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import kh.karazin.foodwise.common.money.Money;
import kh.karazin.foodwise.store.entity.StoreType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Store data transfer object.
 *
 * <p>{@code categoryName} is kept as a deprecated convenience field equal to
 * {@code category.name}; the frontend should migrate to {@code category.slug}
 * within the next sprint. Schedule removal once no caller reads it.
 */
public record StoreDto(
        UUID id,
        String name,
        StoreType type,
        CategoryDto category,
        String description,
        String imageUrl,
        String heroImageUrl,
        String address,
        LocationDto location,
        BigDecimal rating,
        LocalTime opensAt,
        LocalTime closesAt,
        String phone,
        String website,
        Money deliveryFee,
        Money minOrderAmount,
        Integer priceLevel,
        boolean currentlyOpen,
        List<MenuItemDto> menuItems,
        List<ComboDto> combos,
        Double distance
) {

    /**
     * Deprecated mirror of {@code category.name} for transitional backward compatibility.
     * Remove once frontend migrates to {@code category.slug}.
     */
    @JsonProperty("categoryName")
    public String categoryName() {
        return category != null ? category.name() : null;
    }
}
