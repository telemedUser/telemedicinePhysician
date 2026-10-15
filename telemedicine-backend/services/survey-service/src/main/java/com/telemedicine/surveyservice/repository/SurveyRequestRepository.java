package com.telemedicine.surveyservice.repository;

import com.telemedicine.surveyservice.domain.SurveyRequestEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRequestRepository extends JpaRepository<SurveyRequestEntity, String> {
    Optional<SurveyRequestEntity> findByAppointmentId(String appointmentId);

    List<SurveyRequestEntity> findByStatusAndRequestedAtBefore(String status, Instant requestedAt);
}
