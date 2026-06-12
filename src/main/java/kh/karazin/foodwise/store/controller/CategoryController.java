package kh.karazin.foodwise.store.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.dto.CategoryCreateRequest;
import kh.karazin.foodwise.store.dto.CategoryDto;
import kh.karazin.foodwise.store.entity.StoreGroup;
import kh.karazin.foodwise.store.entity.StoreType;
import kh.karazin.foodwise.store.i18n.RequestLocaleResolver;
import kh.karazin.foodwise.store.service.CategoryService;
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

    private final CategoryService categoryService;
    private final RequestLocaleResolver localeResolver;

    @GetMapping
    public ApiResponse<List<CategoryDto>> getCategories(
            @RequestParam(required = false) StoreGroup group,
            @RequestParam(name = "type", required = false) List<StoreType> types,
            HttpServletRequest req) {
        Set<StoreType> typeSet = types != null ? new HashSet<>(types) : Set.of();
        return ApiResponse.success(
                categoryService.findAll(localeResolver.resolve(req), group, typeSet));
    }

    /**
     * Resolve a category by its URL-friendly slug.
     */
    @GetMapping("/{slug}")
    public ApiResponse<CategoryDto> getCategoryBySlug(
            @PathVariable String slug,
            HttpServletRequest req) {
        return ApiResponse.success(
                categoryService.findBySlug(slug, localeResolver.resolve(req)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryDto> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return ApiResponse.success(categoryService.createCategory(request));
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
