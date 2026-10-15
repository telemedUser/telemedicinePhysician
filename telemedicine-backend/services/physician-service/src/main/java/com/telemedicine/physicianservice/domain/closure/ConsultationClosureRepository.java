package com.telemedicine.physicianservice.domain.closure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationClosureRepository
        extends JpaRepository<ConsultationClosureEntity, String> {}
