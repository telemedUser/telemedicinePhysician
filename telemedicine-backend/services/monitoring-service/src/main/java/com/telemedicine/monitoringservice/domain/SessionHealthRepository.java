package com.telemedicine.monitoringservice.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionHealthRepository extends JpaRepository<SessionHealthEntity, String> {

    long countByStatus(String status);

    List<SessionHealthEntity> findByStatus(String status);
}
