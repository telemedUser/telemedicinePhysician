package com.telemedicine.waitingroomservice.repository;

import com.telemedicine.waitingroomservice.domain.AppointmentStatusEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentStatusRepository extends JpaRepository<AppointmentStatusEntity, String> {

    List<AppointmentStatusEntity> findByStatusAndCheckedInAtBefore(String status, Instant cutoff);

    List<AppointmentStatusEntity> findByStatusAndUpdatedAtBefore(String status, Instant cutoff);
}
