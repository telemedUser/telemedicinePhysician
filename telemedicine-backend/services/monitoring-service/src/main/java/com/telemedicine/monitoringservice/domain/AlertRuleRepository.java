package com.telemedicine.monitoringservice.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, String> {

    List<AlertRuleEntity> findByEnabledTrue();
}
