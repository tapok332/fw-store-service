package kh.karazin.foodwise.store.config;

import kh.karazin.foodwise.store.controller.CategoryController;
import kh.karazin.foodwise.store.controller.ComboController;
import kh.karazin.foodwise.store.controller.MenuSectionController;
import kh.karazin.foodwise.store.controller.PromoController;
import kh.karazin.foodwise.store.controller.StoreController;
import kh.karazin.foodwise.store.controller.StoreMenuItemController;
import kh.karazin.foodwise.store.controller.StoreReviewController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for security wiring. Verifies the static metadata that guards
 * write endpoints — annotation-driven contracts that get silently dropped if
 * someone removes them during a refactor.
 *
 * <p>Why metadata-only:
 * <ul>
 *   <li>{@code @WebMvcTest} infrastructure depends on autoconfigure modules
 *       not co-resolved in this build, and a full {@code @SpringBootTest}
 *       drags in JPA/Kafka/Geometry that we don't want for a security check.</li>
 *   <li>The risk we're protecting against is regressions where someone
 *       accidentally drops {@code @PreAuthorize} or {@code @EnableMethodSecurity}.
 *       Reflection catches that without spinning up a context.</li>
 * </ul>
 *
 * <p>End-to-end role-based 401/403 verification is covered by manual smoke /
 * integration tests outside this module.
 */
class SecurityConfigTest {

    @Test
    void securityConfig_hasEnableMethodSecurity() {
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class))
                .as("SecurityConfig must enable @PreAuthorize via @EnableMethodSecurity")
                .isTrue();
    }

    @Test
    void adminWriteEndpoints_arePreAuthorizedAsAdmin() throws NoSuchMethodException {
        assertHasAdminRole(StoreController.class.getDeclaredMethod(
                "createStore", kh.karazin.foodwise.store.dto.StoreCreateRequest.class));
        assertHasAdminRole(CategoryController.class.getDeclaredMethod(
                "createCategory", kh.karazin.foodwise.store.dto.CategoryCreateRequest.class));
        assertHasAdminRole(StoreMenuItemController.class.getDeclaredMethod(
                "createMenuItem", java.util.UUID.class, kh.karazin.foodwise.store.dto.StoreMenuItemDto.class));
        assertHasAdminRole(StoreMenuItemController.class.getDeclaredMethod(
                "updateMenuItem", java.util.UUID.class, java.util.UUID.class,
                kh.karazin.foodwise.store.dto.StoreMenuItemDto.class));
        assertHasAdminRole(StoreMenuItemController.class.getDeclaredMethod(
                "deleteMenuItem", java.util.UUID.class, java.util.UUID.class));
        assertHasAdminRole(PromoController.class.getDeclaredMethod(
                "createPromo", kh.karazin.foodwise.store.dto.PromoAdminDto.class));
        assertHasAdminRole(PromoController.class.getDeclaredMethod(
                "updatePromo", java.util.UUID.class, kh.karazin.foodwise.store.dto.PromoAdminDto.class));
        assertHasAdminRole(PromoController.class.getDeclaredMethod(
                "deletePromo", java.util.UUID.class));
        assertHasAdminRole(ComboController.class.getDeclaredMethod(
                "createCombo", java.util.UUID.class,
                kh.karazin.foodwise.store.dto.ComboCreateRequest.class));
        assertHasAdminRole(MenuSectionController.class.getDeclaredMethod(
                "createSection", java.util.UUID.class,
                kh.karazin.foodwise.store.dto.MenuSectionCreateRequest.class));
    }

    @Test
    void reviewCreate_requiresAuthenticatedUser() throws NoSuchMethodException {
        Method method = StoreReviewController.class.getDeclaredMethod(
                "createReview", java.util.UUID.class, java.util.UUID.class,
                kh.karazin.foodwise.store.dto.ReviewCreateRequest.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("StoreReviewController.createReview must be annotated with @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .as("review create should require any authenticated principal, not admin role")
                .contains("isAuthenticated()");
    }

    private static void assertHasAdminRole(Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("%s must be annotated with @PreAuthorize",
                        method.getDeclaringClass().getSimpleName() + "#" + method.getName())
                .isNotNull();
        assertThat(annotation.value())
                .as("%s must require ROLE_ADMIN",
                        method.getDeclaringClass().getSimpleName() + "#" + method.getName())
                .contains("hasRole('ADMIN')");
    }
}
