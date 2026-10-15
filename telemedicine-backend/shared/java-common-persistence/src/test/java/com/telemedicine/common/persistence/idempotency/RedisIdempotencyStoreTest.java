package com.telemedicine.common.persistence.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.time.FixedTimeProvider;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.web.idempotency.IdempotencyRecord;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisIdempotencyStoreTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    @SuppressWarnings("unchecked")
    void usesSetIfAbsentForReservation() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

        RedisIdempotencyStore store =
                new RedisIdempotencyStore(
                        redisTemplate, EventObjectMappers.create(), new FixedTimeProvider(NOW));

        assertThat(store.tryReserve("pay-1", "fp", Duration.ofMinutes(5))).isTrue();
        verify(valueOperations).setIfAbsent(any(), any(), eq(300_000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void completeRewritesStoredRecord() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ObjectMapper mapper = EventObjectMappers.create();
        IdempotencyRecord inFlight = IdempotencyRecord.inFlight("pay-2", "fp", NOW);
        when(valueOperations.get("telemedicine:idempotency:pay-2"))
                .thenReturn(mapper.writeValueAsString(inFlight));

        RedisIdempotencyStore store =
                new RedisIdempotencyStore(redisTemplate, mapper, new FixedTimeProvider(NOW));
        store.complete("pay-2", 200, "{}", "application/json", Duration.ofMinutes(5));

        verify(valueOperations)
                .set(
                        eq("telemedicine:idempotency:pay-2"),
                        any(),
                        eq(300_000L),
                        eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void releaseDeletesRedisKey() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisIdempotencyStore store =
                new RedisIdempotencyStore(
                        redisTemplate, EventObjectMappers.create(), new FixedTimeProvider(NOW));

        store.release("pay-3");

        verify(redisTemplate).delete("telemedicine:idempotency:pay-3");
    }

    @Test
    @SuppressWarnings("unchecked")
    void completeIgnoresMissingKey() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("telemedicine:idempotency:missing")).thenReturn(null);

        RedisIdempotencyStore store =
                new RedisIdempotencyStore(
                        redisTemplate, EventObjectMappers.create(), new FixedTimeProvider(NOW));
        store.complete("missing", 200, "{}", "application/json", Duration.ofMinutes(5));

        verify(valueOperations, never()).set(any(), any(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findReturnsEmptyWhenMissing() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("telemedicine:idempotency:missing")).thenReturn(null);

        RedisIdempotencyStore store =
                new RedisIdempotencyStore(
                        redisTemplate, EventObjectMappers.create(), new FixedTimeProvider(NOW));

        assertThat(store.find("missing")).isEmpty();
    }
}
