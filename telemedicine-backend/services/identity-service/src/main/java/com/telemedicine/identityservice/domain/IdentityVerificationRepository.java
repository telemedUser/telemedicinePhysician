package com.telemedicine.identityservice.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerificationEntity, String> {

    Optional<IdentityVerificationEntity> findByAppointmentIdAndMethod(String appointmentId, String method);

    Optional<IdentityVerificationEntity> findFirstByAppointmentIdAndValidatedTrueOrderByValidatedAtDesc(
            String appointmentId);
}
