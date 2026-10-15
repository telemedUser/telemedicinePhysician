package com.telemedicine.clinicaldocsservice.repository;

import com.telemedicine.clinicaldocsservice.domain.MedicalLeaveEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalLeaveRepository extends JpaRepository<MedicalLeaveEntity, String> {
    List<MedicalLeaveEntity> findByAppointmentIdOrderByIssuedAtDesc(String appointmentId);
}
