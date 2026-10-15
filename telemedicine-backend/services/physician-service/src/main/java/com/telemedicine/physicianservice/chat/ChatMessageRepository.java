package com.telemedicine.physicianservice.chat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {

    /** Full transcript for a consultation, oldest first (TM-CHT-001 longitudinal order). */
    List<ChatMessageEntity> findByAppointmentIdOrderBySentAtAsc(String appointmentId);
}
