package com.telemedicine.waitingroomservice.repository;

import com.telemedicine.waitingroomservice.domain.SessionLinkEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionLinkRepository extends JpaRepository<SessionLinkEntity, String> {

    Optional<SessionLinkEntity> findFirstByAppointmentIdAndRedeemedAtIsNullOrderByCreatedAtDesc(
            String appointmentId);

    Optional<SessionLinkEntity> findByTokenHash(String tokenHash);
}
