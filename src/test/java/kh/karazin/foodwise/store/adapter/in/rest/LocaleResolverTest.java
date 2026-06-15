package kh.karazin.foodwise.store.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocaleResolverTest {

    private RequestLocaleResolver resolver;
    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        resolver = new RequestLocaleResolver();
        req = mock(HttpServletRequest.class);
    }

    @Test
    void resolve_returnsLocaleFromAcceptLanguage_whenSupported() {
        when(req.getParameter("locale")).thenReturn(null);
        when(req.getLocale()).thenReturn(Locale.forLanguageTag("uk-UA"));

        Locale result = resolver.resolve(req);

        assertThat(result.getLanguage()).isEqualTo("uk");
    }

    @Test
    void resolve_fallsBackToEnglish_whenAcceptLanguageUnsupported() {
        when(req.getParameter("locale")).thenReturn(null);
        when(req.getLocale()).thenReturn(Locale.forLanguageTag("fr"));

        Locale result = resolver.resolve(req);

        assertThat(result.getLanguage()).isEqualTo("en");
    }

    @Test
    void resolve_localeQueryOverridesAcceptLanguage() {
        when(req.getParameter("locale")).thenReturn("uk");
        when(req.getLocale()).thenReturn(Locale.ENGLISH);

        Locale result = resolver.resolve(req);

        assertThat(result.getLanguage()).isEqualTo("uk");
    }

    @Test
    void resolve_throwsOnUnsupportedLocaleQuery() {
        when(req.getParameter("locale")).thenReturn("fr");

        assertThatThrownBy(() -> resolver.resolve(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported locale");
    }

    @Test
    void resolve_blankLocaleQueryFallsToHeader() {
        when(req.getParameter("locale")).thenReturn("");
        when(req.getLocale()).thenReturn(Locale.forLanguageTag("uk"));

        Locale result = resolver.resolve(req);

        assertThat(result.getLanguage()).isEqualTo("uk");
    }
}
