package com.telemedicine.common.persistence.outbox;

import com.telemedicine.common.events.outbox.OutboxEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query(
            value =
                    """
                    SELECT * FROM outbox_events
                    WHERE published = FALSE
                      AND failed = FALSE
                      AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
                    ORDER BY created_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEvent> lockPendingEvents(
            @Param("now") Instant now, @Param("batchSize") int batchSize);

    long countByPublishedFalseAndFailedFalse();
}
