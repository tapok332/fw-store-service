package kh.karazin.foodwise.store.adapter.in.rest;

/**
 * Location data transfer object.
 */
public record LocationDto(
        double latitude,
        double longitude
) {}
