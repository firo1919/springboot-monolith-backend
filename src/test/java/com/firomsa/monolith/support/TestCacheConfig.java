package com.firomsa.monolith.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Provides a no-op {@link CacheManager} for {@code @WebMvcTest} slices.
 *
 * <p>
 * {@code CacheConfig} is guarded by
 * {@code @ConditionalOnProperty(havingValue = "redis")}, and test properties
 * set {@code spring.cache.type=none}. A {@code @WebMvcTest} slice also
 * excludes {@code CacheAutoConfiguration}, so no {@code CacheManager} bean is
 * present — causing a {@code NoSuchBeanDefinitionException} at context startup.
 *
 * <p>
 * Import this class in every {@code @WebMvcTest} unit test via
 * {@code @Import(TestCacheConfig.class)}.
 */
@TestConfiguration
public class TestCacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
