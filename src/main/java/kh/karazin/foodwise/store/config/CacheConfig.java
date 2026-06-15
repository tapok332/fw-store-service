package kh.karazin.foodwise.store.config;

import kh.karazin.foodwise.store.application.CacheNames;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration using simple in-memory cache. Region names are owned by
 * {@link CacheNames} in the application layer so cache-annotated use cases never
 * depend on the config package (ArchUnit-enforced).
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CacheNames.CATEGORIES, CacheNames.STORES,
                CacheNames.HERO_IMAGES, CacheNames.CATEGORY_ICONS);
    }
}
