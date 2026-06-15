package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.store.domain.Promo;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Promo} domain objects to public and admin wire DTOs.
 */
@Component
class PromoRestMapper {

    PromoDto toDto(Promo promo) {
        return new PromoDto(
                promo.id().value(),
                promo.title(),
                promo.description(),
                promo.emoji(),
                promo.bgColor(),
                promo.accentColor(),
                promo.priority());
    }

    PromoAdminDto toAdminDto(Promo promo) {
        return new PromoAdminDto(
                promo.id().value(),
                promo.storeId().value(),
                promo.title(),
                promo.description(),
                promo.emoji(),
                promo.bgColor(),
                promo.accentColor(),
                promo.active(),
                promo.priority());
    }
}
