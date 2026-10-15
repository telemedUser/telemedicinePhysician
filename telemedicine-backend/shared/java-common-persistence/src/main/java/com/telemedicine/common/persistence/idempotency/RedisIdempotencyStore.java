package com.telemedicine.common.persistence.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.time.TimeProvider;
import com.telemedicine.common.web.idempotency.IdempotencyRecord;
import com.telemedicine.common.web.idempotency.IdempotencyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Redis-backed {@link IdempotencyStore} for low-latency idempotent POST handling. */
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "telemedicine:idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TimeProvider clock;

    public RedisIdempotencyStore(
            StringRedisTemplate redisTemplate, ObjectMapper objectMapper, TimeProvider clock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public boolean tryReserve(String key, String fingerprint, Duration ttl) {
        String redisKey = redisKey(key);
        IdempotencyRecord inFlight = IdempotencyRecord.inFlight(key, fingerprint, clock.now());
        Boolean reserved =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                redisKey,
                                serialize(inFlight),
                                ttl.toMillis(),
                                TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(reserved);
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        String value = redisTemplate.opsForValue().get(redisKey(key));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(value));
    }

    @Override
    public void complete(
            String key, int status, String responseBody, String contentType, Duration ttl) {
        Optional<IdempotencyRecord> existing = find(key);
        if (existing.isEmpty()) {
            return;
        }
        IdempotencyRecord completed = existing.get().complete(status, responseBody, contentType);
        redisTemplate
                .opsForValue()
                .set(redisKey(key), serialize(completed), ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void release(String key) {
        redisTemplate.delete(redisKey(key));
    }

    private String redisKey(String key) {
        return KEY_PREFIX + key;
    }

    private String serialize(IdempotencyRecord record) {
        try {
            return objectMapper.writeValueAsString(new StoredRecord(record));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize idempotency record", exception);
        }
    }

    private IdempotencyRecord deserialize(String json) {
        try {
            StoredRecord stored = objectMapper.readValue(json, StoredRecord.class);
            return new IdempotencyRecord(
                    stored.key(),
                    stored.fingerprint(),
                    stored.status(),
                    stored.responseBody(),
                    stored.contentType(),
                    stored.completed(),
                    Instant.parse(stored.createdAt()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize idempotency record", exception);
        }
    }

    private record StoredRecord(
            String key,
            String fingerprint,
            int status,
            String responseBody,
            String contentType,
            boolean completed,
            String createdAt) {

        StoredRecord(IdempotencyRecord record) {
            this(
                    record.key(),
                    record.fingerprint(),
                    record.status(),
                    record.responseBody(),
                    record.contentType(),
                    record.completed(),
                    record.createdAt().toString());
        }
    }
}
