package com.telemedicine.schedulerservice.repository;

import com.telemedicine.schedulerservice.domain.JobDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDefinitionRepository extends JpaRepository<JobDefinition, String> {

    List<JobDefinition> findAllByOrderByJobKeyAsc();
}
