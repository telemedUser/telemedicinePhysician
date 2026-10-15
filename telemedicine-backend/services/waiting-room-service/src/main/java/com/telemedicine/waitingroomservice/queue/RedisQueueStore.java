package com.telemedicine.waitingroomservice.queue;

import com.telemedicine.waitingroomservice.domain.QueueEntryEntity;
import com.telemedicine.waitingroomservice.repository.QueueEntryRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisQueueStore {

    private static final Logger log = LoggerFactory.getLogger(RedisQueueStore.class);

    static final String QUEUE_KEY = "waiting-room:queue";

    private final StringRedisTemplate redisTemplate;
    private final QueueEntryRepository queueEntryRepository;

    public RedisQueueStore(StringRedisTemplate redisTemplate, QueueEntryRepository queueEntryRepository) {
        this.redisTemplate = redisTemplate;
        this.queueEntryRepository = queueEntryRepository;
    }

    public void add(String appointmentId, Instant enteredAt) {
        redisTemplate
                .opsForZSet()
                .add(QUEUE_KEY, appointmentId, enteredAt.toEpochMilli());
    }

    public int position(String appointmentId) {
        rebuildIfMissing();
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, appointmentId);
        return rank == null ? 0 : rank.intValue() + 1;
    }

    public long size() {
        rebuildIfMissing();
        Long size = redisTemplate.opsForZSet().size(QUEUE_KEY);
        return size == null ? 0L : size;
    }

    private void rebuildIfMissing() {
        Long size = redisTemplate.opsForZSet().size(QUEUE_KEY);
        if (size != null && size > 0) {
            return;
        }
        List<QueueEntryEntity> entries = queueEntryRepository.findAllByOrderByEnteredAtAsc();
        if (entries.isEmpty()) {
            return;
        }
        log.info("Rebuilding Redis waiting-room queue from {} database entries", entries.size());
        for (QueueEntryEntity entry : entries) {
            redisTemplate
                    .opsForZSet()
                    .add(QUEUE_KEY, entry.getAppointmentId(), entry.getEnteredAt().toEpochMilli());
        }
    }
}
