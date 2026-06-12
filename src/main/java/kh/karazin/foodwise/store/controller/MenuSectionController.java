package kh.karazin.foodwise.store.controller;

import jakarta.validation.Valid;
import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.dto.MenuSectionCreateRequest;
import kh.karazin.foodwise.store.dto.MenuSectionDto;
import kh.karazin.foodwise.store.service.MenuSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin/seed endpoint for creating per-store menu sections.
 */
@RestController
@RequestMapping("/stores/{storeId}/menu-sections")
@RequiredArgsConstructor
public class MenuSectionController {

    private final MenuSectionService sectionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MenuSectionDto> createSection(
            @PathVariable UUID storeId,
            @Valid @RequestBody MenuSectionCreateRequest request) {
        return ApiResponse.success(sectionService.createSection(storeId, request));
    }
}
