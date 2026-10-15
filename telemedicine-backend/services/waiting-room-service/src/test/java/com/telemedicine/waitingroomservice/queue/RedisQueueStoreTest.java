package com.telemedicine.waitingroomservice.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.waitingroomservice.domain.QueueEntryEntity;
import com.telemedicine.waitingroomservice.repository.QueueEntryRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class RedisQueueStoreTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private QueueEntryRepository queueEntryRepository;
    @Mock private ZSetOperations<String, String> zSetOperations;

    @InjectMocks private RedisQueueStore redisQueueStore;

    @Test
    void rebuildsQueueFromDatabaseWhenRedisIsEmpty() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(RedisQueueStore.QUEUE_KEY)).thenReturn(0L, 1L);
        when(zSetOperations.rank(RedisQueueStore.QUEUE_KEY, "appt-1")).thenReturn(0L);

        QueueEntryEntity entry = new QueueEntryEntity();
        entry.setAppointmentId("appt-1");
        entry.setEnteredAt(Instant.parse("2026-01-01T10:00:00Z"));
        when(queueEntryRepository.findAllByOrderByEnteredAtAsc()).thenReturn(List.of(entry));

        int position = redisQueueStore.position("appt-1");

        assertThat(position).isEqualTo(1);
        verify(zSetOperations).add(eq(RedisQueueStore.QUEUE_KEY), eq("appt-1"), eq(entry.getEnteredAt().toEpochMilli()));
    }
}
