package com.telemedicine.surveyservice.repository;

import com.telemedicine.surveyservice.domain.SurveyResponseEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponseEntity, String> {
    Optional<SurveyResponseEntity> findByAppointmentId(String appointmentId);

    boolean existsByAppointmentId(String appointmentId);
}
