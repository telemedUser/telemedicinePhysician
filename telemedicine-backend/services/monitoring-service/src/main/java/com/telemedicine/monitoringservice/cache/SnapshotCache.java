package com.telemedicine.monitoringservice.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.monitoringservice.config.MonitoringServiceProperties;
import com.telemedicine.monitoringservice.web.dto.MonitoringSnapshot;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SnapshotCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MonitoringServiceProperties properties;

    public SnapshotCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MonitoringServiceProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public Optional<MonitoringSnapshot> get() {
        String cached = redisTemplate.opsForValue().get(properties.getSnapshotCacheKey());
        if (cached == null || cached.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cached, MonitoringSnapshot.class));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    public void put(MonitoringSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            Duration ttl = properties.getSnapshotCacheTtl();
            redisTemplate.opsForValue().set(properties.getSnapshotCacheKey(), json, ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to cache monitoring snapshot", exception);
        }
    }
}
