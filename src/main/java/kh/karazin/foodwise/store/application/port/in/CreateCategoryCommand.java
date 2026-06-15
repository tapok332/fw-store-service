package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.StoreType;

import java.util.Map;
import java.util.Set;

/** Command to create a new category (admin). */
public record CreateCategoryCommand(
        String name,
        String slug,
        String iconName,
        Set<StoreType> applicableTypes,
        Map<String, String> translations
) {
}
