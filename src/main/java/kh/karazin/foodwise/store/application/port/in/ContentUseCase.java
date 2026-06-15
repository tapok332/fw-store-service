package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.CategoryIcon;
import kh.karazin.foodwise.store.domain.HeroImage;

import java.util.List;

/** Hero images and category icons (homepage content management). */
public interface ContentUseCase {

    List<HeroImage> getHeroImages();

    List<CategoryIcon> getCategoryIcons();

    HeroImage createHeroImage(HeroImage heroImage);

    CategoryIcon createCategoryIcon(CategoryIcon categoryIcon);

    void deleteHeroImage(Integer id);

    void deleteCategoryIcon(Integer id);

    HeroImage updateHeroImage(Integer id, HeroImage heroImage);

    CategoryIcon updateCategoryIcon(Integer id, CategoryIcon categoryIcon);
}
