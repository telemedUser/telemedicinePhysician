package com.telemedicine.configservice.repository;

import com.telemedicine.configservice.domain.ConfigRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRevisionRepository extends JpaRepository<ConfigRevision, String> {

    Page<ConfigRevision> findByConfigKey(String configKey, Pageable pageable);
}
