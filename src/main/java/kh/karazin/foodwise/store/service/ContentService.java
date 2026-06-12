package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.store.dto.CategoryIconDto;
import kh.karazin.foodwise.store.dto.HeroImageDto;
import kh.karazin.foodwise.store.entity.CategoryIconEntity;
import kh.karazin.foodwise.store.entity.HeroImageEntity;
import kh.karazin.foodwise.store.repository.CategoryIconRepository;
import kh.karazin.foodwise.store.repository.HeroImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for hero images and category icons (content management).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private static final String HERO_IMAGES_CACHE = "heroImages";
    private static final String CATEGORY_ICONS_CACHE = "categoryIcons";

    private final HeroImageRepository heroImageRepository;
    private final CategoryIconRepository categoryIconRepository;

    @Cacheable(HERO_IMAGES_CACHE)
    @Transactional(readOnly = true)
    public List<HeroImageDto> getHeroImages() {
        return heroImageRepository.findAll().stream()
                .map(e -> HeroImageDto.builder()
                        .id(e.getId())
                        .key(e.getKey())
                        .url(e.getUrl())
                        .icon(e.getIcon())
                        .build())
                .toList();
    }

    @Cacheable(CATEGORY_ICONS_CACHE)
    @Transactional(readOnly = true)
    public List<CategoryIconDto> getCategoryIcons() {
        return categoryIconRepository.findAll().stream()
                .map(e -> CategoryIconDto.builder()
                        .key(e.getKey())
                        .iconName(e.getIconName())
                        .build())
                .toList();
    }

    @CacheEvict(value = HERO_IMAGES_CACHE, allEntries = true)
    @Transactional
    public HeroImageDto createHeroImage(HeroImageDto dto) {
        var entity = HeroImageEntity.builder()
                .key(dto.getKey())
                .url(dto.getUrl())
                .icon(dto.getIcon())
                .build();
        entity = heroImageRepository.save(entity);
        return HeroImageDto.builder()
                .id(entity.getId())
                .key(entity.getKey())
                .url(entity.getUrl())
                .icon(entity.getIcon())
                .build();
    }

    @CacheEvict(value = CATEGORY_ICONS_CACHE, allEntries = true)
    @Transactional
    public CategoryIconDto createCategoryIcon(CategoryIconDto dto) {
        var entity = CategoryIconEntity.builder()
                .key(dto.getKey())
                .iconName(dto.getIconName())
                .build();
        entity = categoryIconRepository.save(entity);
        return CategoryIconDto.builder()
                .key(entity.getKey())
                .iconName(entity.getIconName())
                .build();
    }

    @CacheEvict(value = HERO_IMAGES_CACHE, allEntries = true)
    @Transactional
    public void deleteHeroImage(Integer id) {
        heroImageRepository.deleteById(id);
    }

    @CacheEvict(value = CATEGORY_ICONS_CACHE, allEntries = true)
    @Transactional
    public void deleteCategoryIcon(Integer id) {
        categoryIconRepository.deleteById(id);
    }

    @CacheEvict(value = HERO_IMAGES_CACHE, allEntries = true)
    @Transactional
    public HeroImageDto updateHeroImage(Integer id, HeroImageDto dto) {
        var entity = heroImageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hero image not found: " + id));
        entity.setKey(dto.getKey());
        entity.setUrl(dto.getUrl());
        entity.setIcon(dto.getIcon());
        entity = heroImageRepository.save(entity);
        return HeroImageDto.builder()
                .id(entity.getId())
                .key(entity.getKey())
                .url(entity.getUrl())
                .icon(entity.getIcon())
                .build();
    }

    @CacheEvict(value = CATEGORY_ICONS_CACHE, allEntries = true)
    @Transactional
    public CategoryIconDto updateCategoryIcon(Integer id, CategoryIconDto dto) {
        var entity = categoryIconRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category icon not found: " + id));
        entity.setKey(dto.getKey());
        entity.setIconName(dto.getIconName());
        entity = categoryIconRepository.save(entity);
        return CategoryIconDto.builder()
                .key(entity.getKey())
                .iconName(entity.getIconName())
                .build();
    }
}
