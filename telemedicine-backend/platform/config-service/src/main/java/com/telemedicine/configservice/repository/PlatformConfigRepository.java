package com.telemedicine.configservice.repository;

import com.telemedicine.configservice.domain.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, String> {}
