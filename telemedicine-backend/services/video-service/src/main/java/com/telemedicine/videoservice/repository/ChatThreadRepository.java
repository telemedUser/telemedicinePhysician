package com.telemedicine.videoservice.repository;

import com.telemedicine.videoservice.domain.ChatThreadEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadRepository extends JpaRepository<ChatThreadEntity, String> {

    Optional<ChatThreadEntity> findBySessionId(String sessionId);
}
