package kh.karazin.foodwise.store.adapter.out.persistence;

import kh.karazin.foodwise.store.domain.Category;
import kh.karazin.foodwise.store.domain.CategoryIcon;
import kh.karazin.foodwise.store.domain.CategoryId;
import kh.karazin.foodwise.store.domain.Combo;
import kh.karazin.foodwise.store.domain.ComboId;
import kh.karazin.foodwise.store.domain.HeroImage;
import kh.karazin.foodwise.store.domain.MenuItem;
import kh.karazin.foodwise.store.domain.MenuItemId;
import kh.karazin.foodwise.store.domain.MenuSection;
import kh.karazin.foodwise.store.domain.MenuSectionId;
import kh.karazin.foodwise.store.domain.ProfileId;
import kh.karazin.foodwise.store.domain.Promo;
import kh.karazin.foodwise.store.domain.PromoId;
import kh.karazin.foodwise.store.domain.ReviewId;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreId;
import kh.karazin.foodwise.store.domain.StoreReview;
import kh.karazin.foodwise.store.domain.SurpriseBoxView;
import org.locationtech.jts.geom.Point;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Explicit entity → domain mapping shared by the persistence adapters. The
 * geographic {@code Point} is reduced to plain latitude/longitude here so the
 * domain never sees JTS types. JPA-managed collections are copied into plain
 * collections to fully detach the domain from Hibernate proxies.
 */
final class PersistenceMappers {

    private PersistenceMappers() {
    }

    static Store toStore(StoreEntity e) {
        Double latitude = null;
        Double longitude = null;
        Point location = e.getLocation();
        if (location != null) {
            latitude = location.getY();
            longitude = location.getX();
        }
        Category category = e.getCategory() != null ? toCategory(e.getCategory()) : null;
        return new Store(
                new StoreId(e.getId()),
                e.getName(),
                e.getDescription(),
                e.getImageUrl(),
                e.getHeroImageUrl(),
                e.getType(),
                category,
                e.getAddress(),
                latitude,
                longitude,
                e.getRating(),
                e.getOpensAt(),
                e.getClosesAt(),
                e.getPhone(),
                e.getWebsite(),
                e.getDeliveryFee(),
                e.getMinOrderAmount(),
                e.getPriceLevel());
    }

    static Category toCategory(CategoryEntity e) {
        return new Category(
                new CategoryId(e.getId()),
                e.getName(),
                e.getSlug(),
                e.getIconName(),
                new HashMap<>(e.getTranslations()),
                new HashSet<>(e.getApplicableTypes()));
    }

    static MenuItem toMenuItem(StoreMenuItemEntity e) {
        return new MenuItem(
                new MenuItemId(e.getId()),
                e.getName(),
                e.getDescription(),
                e.getPrice(),
                e.getImageUrl(),
                e.getLegacyCategory(),
                e.getAvailable(),
                new StoreId(e.getStore().getId()),
                e.getSection() != null ? new MenuSectionId(e.getSection().getId()) : null);
    }

    static Combo toCombo(ComboEntity e) {
        List<MenuItem> items = e.getMenuItems() != null
                ? e.getMenuItems().stream().map(PersistenceMappers::toMenuItem).toList()
                : List.of();
        return new Combo(
                new ComboId(e.getId()),
                new StoreId(e.getStore().getId()),
                e.getTitle(),
                e.getPrice(),
                e.getImageUrl(),
                e.getSavings(),
                items);
    }

    static Promo toPromo(PromoEntity e) {
        return new Promo(
                new PromoId(e.getId()),
                new StoreId(e.getStore().getId()),
                e.getTitle(),
                e.getDescription(),
                e.getEmoji(),
                e.getBgColor(),
                e.getAccentColor(),
                e.getActive(),
                e.getPriority());
    }

    static MenuSection toMenuSection(MenuSectionEntity e) {
        return new MenuSection(
                new MenuSectionId(e.getId()),
                new StoreId(e.getStore().getId()),
                e.getTitle(),
                e.getSortOrder());
    }

    static StoreReview toStoreReview(StoreReviewEntity e) {
        return new StoreReview(
                new ReviewId(e.getId()),
                new StoreId(e.getStore().getId()),
                new ProfileId(e.getProfileId()),
                e.getOrderId(),
                e.getRating(),
                e.getComment(),
                e.getCreatedAt());
    }

    static HeroImage toHeroImage(HeroImageEntity e) {
        return new HeroImage(e.getId(), e.getKey(), e.getUrl(), e.getIcon());
    }

    static CategoryIcon toCategoryIcon(CategoryIconEntity e) {
        return new CategoryIcon(e.getId(), e.getKey(), e.getIconName());
    }

    static SurpriseBoxView toSurpriseBoxView(SurpriseBoxEntity e) {
        StoreEntity store = e.getStore();
        Double latitude = null;
        Double longitude = null;
        if (store.getLocation() != null) {
            latitude = store.getLocation().getY();
            longitude = store.getLocation().getX();
        }
        return new SurpriseBoxView(
                e.getId(),
                store.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getImageUrl(),
                e.getPrice(),
                e.getRetailPrice(),
                e.getStock(),
                e.getPickupFrom(),
                e.getPickupTo(),
                e.getDeliveryAvailable(),
                e.getCategory(),
                e.getRating(),
                store.getName(),
                store.getAddress(),
                latitude,
                longitude);
    }
}
