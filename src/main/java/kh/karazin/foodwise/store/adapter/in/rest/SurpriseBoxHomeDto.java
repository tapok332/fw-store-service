package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.common.money.Money;

import java.time.LocalTime;

/**
 * Simplified SurpriseBox DTO for home screen.
 */
public record SurpriseBoxHomeDto(
        String id,
        String title,
        String description,
        String imageUrl,
        Money price,
        Money retailPrice,
        int stock,
        LocalTime pickupFrom,
        LocalTime pickupTo,
        boolean deliveryAvailable,
        String category,
        Double rating,
        int discount,
        String storeId,
        String storeName,
        String storeAddress,
        Double storeLatitude,
        Double storeLongitude
) {}
