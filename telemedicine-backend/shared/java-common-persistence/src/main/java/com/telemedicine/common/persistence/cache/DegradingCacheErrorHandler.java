package com.telemedicine.common.persistence.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/** Treats cache failures as a miss instead of failing the request when Redis is unavailable. */
public class DegradingCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(DegradingCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
                "Cache get failed for {} key {} — treating as miss",
                cache.getName(),
                key,
                exception);
    }

    @Override
    public void handleCachePutError(
            RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache put failed for {} key {} — continuing", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache evict failed for {} key {} — continuing", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache clear failed for {} — continuing", cache.getName(), exception);
    }
}
