package kh.karazin.foodwise.store.domain;

/**
 * Raised when a {@link Combo} would be composed of menu items that do not all
 * belong to the combo's store. Translated to an HTTP 400 by the use case.
 */
public class ComboCompositionException extends RuntimeException {

    public ComboCompositionException(String message) {
        super(message);
    }
}
