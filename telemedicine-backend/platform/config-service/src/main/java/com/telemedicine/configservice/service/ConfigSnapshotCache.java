package com.telemedicine.configservice.service;

import com.telemedicine.configservice.dto.ConfigSnapshotDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class ConfigSnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(ConfigSnapshotCache.class);
    public static final String CACHE_NAME = "configSnapshot";

    private final CacheManager cacheManager;
    private final String cacheKey;

    public ConfigSnapshotCache(
            CacheManager cacheManager,
            @Value("${telemedicine.config.snapshot-cache-key:platform-config-snapshot}")
                    String cacheKey) {
        this.cacheManager = cacheManager;
        this.cacheKey = cacheKey;
    }

    public ConfigSnapshotDto get() {
        try {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache == null) {
                return null;
            }
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper == null) {
                return null;
            }
            Object value = wrapper.get();
            if (value instanceof ConfigSnapshotDto snapshot) {
                return snapshot;
            }
            return null;
        } catch (Exception exception) {
            log.warn("Redis cache read failed, falling back to database", exception);
            return null;
        }
    }

    public void put(ConfigSnapshotDto snapshot) {
        try {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.put(cacheKey, snapshot);
            }
        } catch (Exception exception) {
            log.warn("Redis cache write failed, continuing without cache", exception);
        }
    }

    public void evict() {
        try {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.evict(cacheKey);
            }
        } catch (Exception exception) {
            log.warn("Redis cache eviction failed", exception);
        }
    }
}
