package com.telemedicine.configservice.config;

import com.telemedicine.configservice.service.ConfigSnapshotCache;
import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class ConfigServiceCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceCacheConfig.class);

    @Bean
    RedisCacheManagerBuilderCustomizer configSnapshotCacheCustomizer() {
        return builder ->
                builder.withCacheConfiguration(
                        ConfigSnapshotCache.CACHE_NAME,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(60)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache get failed for {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(
                    RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache put failed for {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache evict failed for {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache clear failed: {}", exception.getMessage());
            }
        };
    }
}
