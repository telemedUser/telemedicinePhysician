package com.telemedicine.clinicaldocsservice.repository;

import com.telemedicine.clinicaldocsservice.domain.PrescriptionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, String> {
    List<PrescriptionEntity> findByAppointmentIdOrderByIssuedAtDesc(String appointmentId);
}
