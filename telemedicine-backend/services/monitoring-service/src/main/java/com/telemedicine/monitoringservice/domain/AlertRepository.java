package com.telemedicine.monitoringservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, String> {

    long countByStatus(String status);
}
