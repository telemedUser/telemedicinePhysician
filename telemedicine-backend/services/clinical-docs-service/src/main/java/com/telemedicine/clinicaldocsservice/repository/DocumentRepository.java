package com.telemedicine.clinicaldocsservice.repository;

import com.telemedicine.clinicaldocsservice.domain.DocumentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {
    List<DocumentEntity> findByAppointmentIdOrderByIssuedAtDesc(String appointmentId);

    Optional<DocumentEntity> findTopByAppointmentIdOrderByIssuedAtDesc(String appointmentId);
}
