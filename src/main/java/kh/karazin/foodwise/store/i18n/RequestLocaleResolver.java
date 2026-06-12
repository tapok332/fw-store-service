package kh.karazin.foodwise.store.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Resolves the request locale from {@code ?locale=} (preferred) or
 * {@code Accept-Language} header, falling back to English if the requested
 * language is not in {@link #SUPPORTED}.
 *
 * <p>Throws {@link IllegalArgumentException} for non-blank but unsupported
 * {@code ?locale=} values — caught by controller exception handlers as 400.
 *
 * <p>Named {@code RequestLocaleResolver} (not {@code LocaleResolver}) to avoid
 * clashing with Spring MVC's built-in {@code LocaleResolver} bean name.
 */
@Component
public class RequestLocaleResolver {

    public static final Set<String> SUPPORTED = Set.of("en", "uk");
    public static final Locale FALLBACK = Locale.ENGLISH;

    public Locale resolve(HttpServletRequest req) {
        String explicit = req.getParameter("locale");
        if (explicit != null && !explicit.isBlank()) {
            if (!SUPPORTED.contains(explicit)) {
                throw new IllegalArgumentException("Unsupported locale: " + explicit);
            }
            return Locale.forLanguageTag(explicit);
        }
        Locale fromHeader = req.getLocale();
        if (fromHeader != null && SUPPORTED.contains(fromHeader.getLanguage())) {
            return fromHeader;
        }
        return FALLBACK;
    }
}
