package com.firomsa.monolith.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
public class CacheConfig {

    public static final String EMPLOYEES = "employees";

    private final CacheProperties cacheProperties;

    public CacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * Full Redis-backed cache manager used in production and when Redis is
     * available.
     * Only activated when {@code spring.cache.type=redis} (or the property is
     * absent,
     * which means the application is running in its normal production mode).
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnBean(RedisConnectionFactory.class)
    RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        SerializationPair<Object> jsonSerializer = SerializationPair.fromSerializer(RedisSerializer.json());

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(jsonSerializer);

        Map<String, RedisCacheConfiguration> caches = Map.of(
                EMPLOYEES, base.entryTtl(Duration.ofMinutes(cacheProperties.getEmployees())));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(cacheProperties.getEmployees())))
                .withInitialCacheConfigurations(caches)
                .build();
    }

    /**
     * No-op cache manager used during tests when {@code spring.cache.type=none}.
     * Satisfies the {@code CacheManager} dependency required by
     * {@code @EnableCaching}
     * without needing a running Redis instance.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "none")
    CacheManager noOpCacheManager() {
        return new NoOpCacheManager();
    }
}
