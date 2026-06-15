package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.store.application.CacheNames;
import kh.karazin.foodwise.store.application.port.in.ContentUseCase;
import kh.karazin.foodwise.store.application.port.out.CategoryIconRepository;
import kh.karazin.foodwise.store.application.port.out.HeroImageRepository;
import kh.karazin.foodwise.store.domain.CategoryIcon;
import kh.karazin.foodwise.store.domain.HeroImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hero images and category icons (homepage content management).
 */
@Slf4j
@Service
@RequiredArgsConstructor
class ContentService implements ContentUseCase {

    private final HeroImageRepository heroImageRepository;
    private final CategoryIconRepository categoryIconRepository;

    @Override
    @Cacheable(CacheNames.HERO_IMAGES)
    @Transactional(readOnly = true)
    public List<HeroImage> getHeroImages() {
        return heroImageRepository.findAll();
    }

    @Override
    @Cacheable(CacheNames.CATEGORY_ICONS)
    @Transactional(readOnly = true)
    public List<CategoryIcon> getCategoryIcons() {
        return categoryIconRepository.findAll();
    }

    @Override
    @CacheEvict(value = CacheNames.HERO_IMAGES, allEntries = true)
    @Transactional
    public HeroImage createHeroImage(HeroImage heroImage) {
        return heroImageRepository.save(heroImage);
    }

    @Override
    @CacheEvict(value = CacheNames.CATEGORY_ICONS, allEntries = true)
    @Transactional
    public CategoryIcon createCategoryIcon(CategoryIcon categoryIcon) {
        return categoryIconRepository.save(categoryIcon);
    }

    @Override
    @CacheEvict(value = CacheNames.HERO_IMAGES, allEntries = true)
    @Transactional
    public void deleteHeroImage(Integer id) {
        heroImageRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = CacheNames.CATEGORY_ICONS, allEntries = true)
    @Transactional
    public void deleteCategoryIcon(Integer id) {
        categoryIconRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = CacheNames.HERO_IMAGES, allEntries = true)
    @Transactional
    public HeroImage updateHeroImage(Integer id, HeroImage heroImage) {
        heroImageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hero image not found: " + id));
        return heroImageRepository.save(new HeroImage(id, heroImage.key(), heroImage.url(), heroImage.icon()));
    }

    @Override
    @CacheEvict(value = CacheNames.CATEGORY_ICONS, allEntries = true)
    @Transactional
    public CategoryIcon updateCategoryIcon(Integer id, CategoryIcon categoryIcon) {
        categoryIconRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category icon not found: " + id));
        return categoryIconRepository.save(new CategoryIcon(id, categoryIcon.key(), categoryIcon.iconName()));
    }
}
