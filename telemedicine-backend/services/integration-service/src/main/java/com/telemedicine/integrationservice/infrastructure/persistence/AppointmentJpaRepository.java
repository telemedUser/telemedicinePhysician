package com.telemedicine.integrationservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, String> {}
