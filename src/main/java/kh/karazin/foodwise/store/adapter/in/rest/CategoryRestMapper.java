package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.store.domain.Category;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Maps {@link Category} domain objects to their wire DTO, resolving the
 * localized display name per request locale.
 */
@Component
class CategoryRestMapper {

    CategoryDto toDto(Category category, Locale locale) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(
                category.id().value(),
                category.slug(),
                category.resolveName(locale),
                category.iconName(),
                category.sortedTypes(),
                category.applicableGroups());
    }
}
