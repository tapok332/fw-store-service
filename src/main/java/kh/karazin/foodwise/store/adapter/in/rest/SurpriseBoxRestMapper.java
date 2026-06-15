package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.store.domain.SurpriseBoxView;
import org.springframework.stereotype.Component;

/**
 * Maps {@link SurpriseBoxView} read projections to the home-screen wire DTO.
 */
@Component
class SurpriseBoxRestMapper {

    SurpriseBoxHomeDto toHomeDto(SurpriseBoxView box) {
        return new SurpriseBoxHomeDto(
                box.id().toString(),
                box.title(),
                box.description(),
                box.imageUrl(),
                box.price(),
                box.retailPrice(),
                box.stock(),
                box.pickupFrom(),
                box.pickupTo(),
                box.deliveryAvailable(),
                box.category(),
                box.rating() != null ? box.rating().doubleValue() : null,
                box.discountPercentage(),
                box.storeId().toString(),
                box.storeName(),
                box.storeAddress(),
                box.storeLatitude(),
                box.storeLongitude());
    }
}
