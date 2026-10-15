package com.telemedicine.common.persistence.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.persistence.TelemedicinePersistenceProperties;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(
        prefix = "telemedicine.persistence.cache",
        name = "enabled",
        matchIfMissing = true)
public class TelemedicineCacheConfiguration implements CachingConfigurer {

    private final DegradingCacheErrorHandler cacheErrorHandler = new DegradingCacheErrorHandler();

    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler;
    }

    @Bean
    RedisCacheManager telemedicineRedisCacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            TelemedicinePersistenceProperties properties) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper.copy());
        RedisCacheConfiguration defaults =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(properties.getCache().getDefaultTtl())
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        serializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        CacheNames.defaultTtls(properties.getCache().getDefaultTtl())
                .forEach((name, ttl) -> perCache.put(name, defaults.entryTtl(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Bean
    DegradingCacheErrorHandler telemedicineCacheErrorHandler() {
        return cacheErrorHandler;
    }
}
