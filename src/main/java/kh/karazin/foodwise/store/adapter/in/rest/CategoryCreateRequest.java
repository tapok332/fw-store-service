package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kh.karazin.foodwise.store.domain.StoreType;

import java.util.Map;
import java.util.Set;

/**
 * Request body for creating a new store category.
 *
 * <p>If {@code slug} is omitted the service derives it from {@code name}
 * (lowercase, non-alnum collapsed to hyphens).
 *
 * <p>{@code translations} <b>must</b> contain an {@code "en"} entry — the
 * English label is the resolver's last fallback before the canonical code.
 * {@code applicableTypes} must be non-empty.
 */
public record CategoryCreateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Pattern(regexp = "^[a-z0-9-]+$", message = "slug must match ^[a-z0-9-]+$")
        @Size(max = 64)
        String slug,

        @Size(max = 64)
        String iconName,

        @NotEmpty(message = "applicableTypes must not be empty")
        Set<StoreType> applicableTypes,

        @NotNull
        Map<String, String> translations
) {
    @AssertTrue(message = "translations must include an 'en' entry")
    public boolean isEnglishTranslationPresent() {
        return translations != null
                && translations.containsKey("en")
                && translations.get("en") != null
                && !translations.get("en").isBlank();
    }
}
