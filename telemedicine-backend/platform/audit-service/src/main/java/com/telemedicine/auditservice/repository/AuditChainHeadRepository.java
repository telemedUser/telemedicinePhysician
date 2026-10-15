package com.telemedicine.auditservice.repository;

import com.telemedicine.auditservice.domain.AuditChainHeadEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditChainHeadRepository extends JpaRepository<AuditChainHeadEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from AuditChainHeadEntity h where h.id = :id")
    Optional<AuditChainHeadEntity> findAndLockById(@Param("id") String id);
}
