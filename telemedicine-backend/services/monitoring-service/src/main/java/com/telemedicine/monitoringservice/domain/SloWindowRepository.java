package com.telemedicine.monitoringservice.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SloWindowRepository extends JpaRepository<SloWindowEntity, String> {

    Optional<SloWindowEntity> findFirstByOrderByWindowEndDesc();
}
