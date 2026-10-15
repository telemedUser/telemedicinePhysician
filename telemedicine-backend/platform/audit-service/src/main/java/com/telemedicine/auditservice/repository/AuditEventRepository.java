package com.telemedicine.auditservice.repository;

import com.telemedicine.auditservice.domain.AuditEventEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository
        extends JpaRepository<AuditEventEntity, String>, JpaSpecificationExecutor<AuditEventEntity> {

    List<AuditEventEntity> findAllByOrderBySequenceAsc();

    Page<AuditEventEntity> findByActorId(String actorId, Pageable pageable);

    Page<AuditEventEntity> findByAction(String action, Pageable pageable);

    Page<AuditEventEntity> findByResourceType(String resourceType, Pageable pageable);

    Page<AuditEventEntity> findByOccurredAtBetween(Instant from, Instant to, Pageable pageable);
}
