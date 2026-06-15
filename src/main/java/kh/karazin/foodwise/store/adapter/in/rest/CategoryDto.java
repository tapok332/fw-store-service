package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;

import java.util.List;
import java.util.UUID;

/**
 * Category data transfer object.
 *
 * @param id                UUID
 * @param slug              URL-friendly stable identifier (e.g. {@code "pizza"})
 * @param name              localized display name; resolved per request locale
 *                          (Accept-Language / ?locale=) with fallback to 'en'
 * @param iconName          optional icon hint for the frontend icon set
 * @param applicableTypes   non-empty list of {@link StoreType}s this category
 *                          applies to (e.g. cuisine "Bakery" → RESTAURANT+BAKERY)
 * @param applicableGroups  derived from {@code applicableTypes}, deduplicated
 */
public record CategoryDto(
        UUID id,
        String slug,
        String name,
        String iconName,
        List<StoreType> applicableTypes,
        List<StoreGroup> applicableGroups
) {}
