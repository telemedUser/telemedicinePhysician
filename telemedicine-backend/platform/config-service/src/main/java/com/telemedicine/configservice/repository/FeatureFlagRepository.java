package com.telemedicine.configservice.repository;

import com.telemedicine.configservice.domain.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {}
