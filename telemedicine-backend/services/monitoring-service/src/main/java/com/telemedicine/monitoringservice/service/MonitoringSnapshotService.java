package com.telemedicine.monitoringservice.service;

import com.telemedicine.monitoringservice.cache.SnapshotCache;
import com.telemedicine.monitoringservice.web.dto.MonitoringSnapshot;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MonitoringSnapshotService {

    private static final String WAITING_QUEUE_KEY = "monitoring:waiting-queue";

    private final SessionHealthService sessionHealthService;
    private final SloService sloService;
    private final AlertService alertService;
    private final SnapshotCache snapshotCache;
    private final StringRedisTemplate redisTemplate;

    public MonitoringSnapshotService(
            SessionHealthService sessionHealthService,
            SloService sloService,
            AlertService alertService,
            SnapshotCache snapshotCache,
            StringRedisTemplate redisTemplate) {
        this.sessionHealthService = sessionHealthService;
        this.sloService = sloService;
        this.alertService = alertService;
        this.snapshotCache = snapshotCache;
        this.redisTemplate = redisTemplate;
    }

    public MonitoringSnapshot snapshot() {
        return snapshotCache.get().orElseGet(this::buildAndCacheSnapshot);
    }

    public MonitoringSnapshot refreshSnapshot() {
        return buildAndCacheSnapshot();
    }

    public void incrementWaitingQueue() {
        redisTemplate.opsForValue().increment(WAITING_QUEUE_KEY);
    }

    public void decrementWaitingQueue() {
        Long current = redisTemplate.opsForValue().decrement(WAITING_QUEUE_KEY);
        if (current != null && current < 0) {
            redisTemplate.opsForValue().set(WAITING_QUEUE_KEY, "0");
        }
    }

    private MonitoringSnapshot buildAndCacheSnapshot() {
        MonitoringSnapshot snapshot =
                new MonitoringSnapshot(
                        (int) sessionHealthService.countActiveSessions(),
                        waitingRoomQueue(),
                        sloService.currentErrorRate(),
                        sessionHealthService.averageConnectionQuality(),
                        (int) alertService.openAlertCount(),
                        sloService.currentBurnRate());
        snapshotCache.put(snapshot);
        alertService.evaluateErrorRate(snapshot.errorRate());
        return snapshot;
    }

    private int waitingRoomQueue() {
        String value = redisTemplate.opsForValue().get(WAITING_QUEUE_KEY);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
