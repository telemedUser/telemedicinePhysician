package com.telemedicine.auditservice.repository;

import com.telemedicine.auditservice.domain.ConsentLedgerEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentLedgerRepository extends JpaRepository<ConsentLedgerEntity, String> {

    List<ConsentLedgerEntity> findByAppointmentIdOrderByRecordedAtDesc(String appointmentId);

    List<ConsentLedgerEntity> findByRecordedAtBefore(Instant cutoff);
}
