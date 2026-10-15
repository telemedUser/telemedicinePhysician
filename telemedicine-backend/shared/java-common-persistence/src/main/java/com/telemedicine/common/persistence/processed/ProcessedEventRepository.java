package com.telemedicine.common.persistence.processed;

import com.telemedicine.common.persistence.processed.ProcessedEventEntity.ProcessedEventId;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEventEntity, ProcessedEventId> {

    @Modifying
    @Query("DELETE FROM ProcessedEventEntity e WHERE e.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
