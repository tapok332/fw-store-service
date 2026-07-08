package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.store.application.port.in.StoreSearchParams;
import kh.karazin.foodwise.store.domain.StoreGroup;
import kh.karazin.foodwise.store.domain.StoreType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request body for the HTTP {@code QUERY /stores} endpoint.
 *
 * <p>{@code QUERY} (IETF {@code draft-ietf-httpbis-safe-method-w-body}) is a
 * safe, idempotent method like {@code GET}, but it carries its parameters in a
 * body instead of the query string. That lets a structured filter — notably the
 * geo box as a nested object — travel as JSON rather than as a long, encoded
 * {@code ?latitude=..&longitude=..&maxDistance=..} tail.
 *
 * <p>This is a driving-adapter DTO: it maps 1:1 onto the input-port query object
 * {@link StoreSearchParams} and never leaks into the domain.
 */
public record StoreSearchQuery(
        String search,
        StoreType type,
        StoreGroup group,
        UUID categoryId,
        String categorySlug,
        BigDecimal minRating,
        List<Integer> priceLevels,
        Boolean openNow,
        GeoFilter within,
        String sort,
        Integer page,
        Integer size
) {

    /** Distance filter expressed as a point plus a radius in kilometers. */
    public record GeoFilter(Double lat, Double lng, Double radiusKm) {}

    /**
     * Translate this wire model into the input-port query object. Unknown
     * {@code sort} values and the {@code type}/{@code group} conflict surface as
     * {@link IllegalArgumentException} here, exactly as they do for {@code GET}.
     */
    public StoreSearchParams toParams() {
        Double lat = within != null ? within.lat() : null;
        Double lng = within != null ? within.lng() : null;
        Double radiusKm = within != null ? within.radiusKm() : null;

        return new StoreSearchParams(
                search, type, group, categoryId, categorySlug,
                lat, lng, minRating, radiusKm, openNow, priceLevels,
                StoreSearchParams.SortField.parse(sort),
                page != null ? page : 0,
                size != null ? size : 20);
    }
}
