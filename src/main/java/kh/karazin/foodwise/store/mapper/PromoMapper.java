package kh.karazin.foodwise.store.mapper;

import kh.karazin.foodwise.store.dto.PromoAdminDto;
import kh.karazin.foodwise.store.dto.PromoDto;
import kh.karazin.foodwise.store.entity.PromoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting promo entities to DTOs.
 */
@Component
public class PromoMapper {

    public PromoDto toDto(PromoEntity entity) {
        return new PromoDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getEmoji(),
                entity.getBgColor(),
                entity.getAccentColor(),
                entity.getPriority()
        );
    }

    public PromoAdminDto toAdminDto(PromoEntity entity) {
        return new PromoAdminDto(
                entity.getId(),
                entity.getStore().getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getEmoji(),
                entity.getBgColor(),
                entity.getAccentColor(),
                entity.getActive(),
                entity.getPriority()
        );
    }
}
