package kh.karazin.foodwise.store.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration using simple in-memory cache.
 */
@Configuration
public class CacheConfig {

    public static final String CATEGORIES_CACHE = "categories";
    public static final String STORES_CACHE = "stores";
    public static final String HERO_IMAGES_CACHE = "heroImages";
    public static final String CATEGORY_ICONS_CACHE = "categoryIcons";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CATEGORIES_CACHE, STORES_CACHE,
                HERO_IMAGES_CACHE, CATEGORY_ICONS_CACHE);
    }
}
