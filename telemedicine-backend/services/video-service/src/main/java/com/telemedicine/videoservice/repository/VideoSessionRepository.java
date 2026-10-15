package com.telemedicine.videoservice.repository;

import com.telemedicine.videoservice.domain.VideoSessionEntity;
import com.telemedicine.videoservice.domain.VideoSessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoSessionRepository extends JpaRepository<VideoSessionEntity, String> {

    Optional<VideoSessionEntity> findByAppointmentId(String appointmentId);

    List<VideoSessionEntity> findByStatusNotAndExpiresAtBefore(
            VideoSessionStatus status, Instant expiresAt);
}
