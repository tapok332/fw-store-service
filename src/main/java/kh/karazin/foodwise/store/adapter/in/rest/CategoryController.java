package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.application.port.in.CategoryUseCase;
import kh.karazin.foodwise.store.application.port.in.CreateCategoryCommand;
import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Controller for store categories. Supports filtering by {@link StoreGroup}
 * or by a set of {@link StoreType} (mutually exclusive) and localization of
 * the {@code name} field via {@code Accept-Language} or {@code ?locale=}.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryUseCase categoryUseCase;
    private final RequestLocaleResolver localeResolver;
    private final CategoryRestMapper categoryRestMapper;

    @GetMapping
    public ApiResponse<List<CategoryDto>> getCategories(
            @RequestParam(required = false) StoreGroup group,
            @RequestParam(name = "type", required = false) List<StoreType> types,
            HttpServletRequest req) {
        Set<StoreType> typeSet = types != null ? new HashSet<>(types) : Set.of();
        Locale locale = localeResolver.resolve(req);
        List<CategoryDto> result = categoryUseCase.findAll(group, typeSet).stream()
                .map(c -> categoryRestMapper.toDto(c, locale))
                .toList();
        return ApiResponse.success(result);
    }

    /**
     * Resolve a category by its URL-friendly slug.
     */
    @GetMapping("/{slug}")
    public ApiResponse<CategoryDto> getCategoryBySlug(
            @PathVariable String slug,
            HttpServletRequest req) {
        return ApiResponse.success(
                categoryRestMapper.toDto(categoryUseCase.findBySlug(slug), localeResolver.resolve(req)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryDto> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CreateCategoryCommand command = new CreateCategoryCommand(
                request.name(), request.slug(), request.iconName(),
                request.applicableTypes(), request.translations());
        return ApiResponse.success(
                categoryRestMapper.toDto(categoryUseCase.createCategory(command), Locale.ENGLISH));
    }

    /**
     * Translate IllegalArgumentException from LocaleResolver / service into 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error("BAD_REQUEST", e.getMessage());
    }
}
