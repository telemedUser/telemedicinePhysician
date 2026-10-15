package com.telemedicine.reportingservice.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JourneyMilestoneRepository
        extends JpaRepository<JourneyMilestoneEntity, JourneyMilestoneEntity.JourneyMilestoneId> {

    Optional<JourneyMilestoneEntity> findByAppointmentIdAndMilestone(
            String appointmentId, String milestone);
}
