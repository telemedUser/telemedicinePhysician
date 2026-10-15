package com.telemedicine.videoservice.repository;

import com.telemedicine.videoservice.domain.SessionTokenEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTokenRepository extends JpaRepository<SessionTokenEntity, String> {

    List<SessionTokenEntity> findByExpiresAtBefore(Instant expiresAt);
}
