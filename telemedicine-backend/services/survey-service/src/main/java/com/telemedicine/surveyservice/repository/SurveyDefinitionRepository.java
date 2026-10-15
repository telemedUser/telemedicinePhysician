package com.telemedicine.surveyservice.repository;

import com.telemedicine.surveyservice.domain.SurveyDefinitionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyDefinitionRepository extends JpaRepository<SurveyDefinitionEntity, String> {
    Optional<SurveyDefinitionEntity> findFirstByNameAndActiveTrueOrderBySurveyVersionDesc(String name);
}
