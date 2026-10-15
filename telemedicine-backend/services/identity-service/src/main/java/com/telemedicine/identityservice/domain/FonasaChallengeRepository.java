package com.telemedicine.identityservice.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FonasaChallengeRepository extends JpaRepository<FonasaChallengeEntity, String> {

    Optional<FonasaChallengeEntity> findFirstByAppointmentIdOrderByCreatedAtDesc(String appointmentId);
}
