package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.store.domain.CategoryIcon;
import kh.karazin.foodwise.store.domain.HeroImage;
import org.springframework.stereotype.Component;

/**
 * Maps content domain objects (hero images, category icons) to their wire DTOs.
 */
@Component
class ContentRestMapper {

    HeroImageDto toDto(HeroImage heroImage) {
        return HeroImageDto.builder()
                .id(heroImage.id() != null ? heroImage.id() : 0)
                .key(heroImage.key())
                .url(heroImage.url())
                .icon(heroImage.icon())
                .build();
    }

    CategoryIconDto toDto(CategoryIcon categoryIcon) {
        return CategoryIconDto.builder()
                .key(categoryIcon.key())
                .iconName(categoryIcon.iconName())
                .build();
    }
}
