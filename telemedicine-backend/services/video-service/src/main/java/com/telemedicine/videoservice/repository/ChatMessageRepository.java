package com.telemedicine.videoservice.repository;

import com.telemedicine.videoservice.domain.ChatMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {

    List<ChatMessageEntity> findBySessionIdOrderBySentAtAsc(String sessionId);
}
