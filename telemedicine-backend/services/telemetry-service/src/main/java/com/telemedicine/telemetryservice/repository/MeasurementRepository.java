package com.telemedicine.telemetryservice.repository;

import com.telemedicine.telemetryservice.domain.MeasurementEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementRepository extends JpaRepository<MeasurementEntity, String> {

    List<MeasurementEntity> findByAppointmentId(String appointmentId);
}
