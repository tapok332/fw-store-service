package kh.karazin.foodwise.store.application.port.in;

/**
 * Command to update an existing promo. {@code active} and {@code priority} are
 * optional — {@code null} values leave the current values unchanged.
 */
public record UpdatePromoCommand(
        String title,
        String description,
        String emoji,
        String bgColor,
        String accentColor,
        Boolean active,
        Integer priority
) {
}
