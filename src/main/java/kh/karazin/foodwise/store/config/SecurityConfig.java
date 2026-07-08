package kh.karazin.foodwise.store.config;

import kh.karazin.foodwise.common.security.InternalAuthFilter;
import kh.karazin.foodwise.common.security.XUserHeadersAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.List;

/**
 * Security configuration for the store-service.
 *
 * <h3>Filter chain</h3>
 * <ol>
 *   <li>{@link InternalAuthFilter} — gates every non-{@code /actuator/health} request
 *       behind a shared {@code X-Internal-Token} header. Direct external access
 *       (bypassing the gateway) is rejected with HTTP 403.</li>
 *   <li>{@link XUserHeadersAuthFilter} — reads gateway-injected
 *       {@code X-User-Id} / {@code X-User-Roles} headers and populates the
 *       Spring SecurityContext so that {@code @PreAuthorize} expressions work.
 *       Skips {@code /internal/**} and {@code /actuator/**}.</li>
 * </ol>
 *
 * <h3>Authorization model</h3>
 * Endpoint-level checks are expressed as {@code @PreAuthorize} on controller
 * methods (enabled by {@link EnableMethodSecurity}). The HTTP rules below
 * permit all paths because gateway-level auth ({@code InternalAuthFilter}) +
 * method-level authorization ({@code @PreAuthorize hasRole(...)}) provide
 * defence in depth without duplicating the rules in two places.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public XUserHeadersAuthFilter xUserHeadersAuthFilter() {
        return new XUserHeadersAuthFilter();
    }

    /**
     * {@link StrictHttpFirewall} rejects any method outside its default allow-list
     * ({@code GET/HEAD/POST/PUT/PATCH/DELETE/OPTIONS/TRACE}) with 400 before the
     * request ever reaches the {@code DispatcherServlet}. The HTTP {@code QUERY}
     * method (see {@code StoreQueryRouterConfig}) must be added explicitly.
     */
    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowedHttpMethods(List.of(
                "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "QUERY"));
        return firewall;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           XUserHeadersAuthFilter xUserHeadersAuthFilter,
                                           @Value("${internal.service.secret}") String internalSecret) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new InternalAuthFilter(internalSecret), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(xUserHeadersAuthFilter, InternalAuthFilter.class)
                .build();
    }
}
