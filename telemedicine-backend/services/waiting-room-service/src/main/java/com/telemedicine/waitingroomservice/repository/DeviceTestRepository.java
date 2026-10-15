package com.telemedicine.waitingroomservice.repository;

import com.telemedicine.waitingroomservice.domain.DeviceTestEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTestRepository extends JpaRepository<DeviceTestEntity, String> {

    Optional<DeviceTestEntity> findByAppointmentId(String appointmentId);
}
