package com.telemedicine.physicianservice.chat;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.EventPublisher;
import com.telemedicine.common.events.payload.ChatMessageSent;
import com.telemedicine.physicianservice.chat.dto.ChatMessageDto;
import com.telemedicine.physicianservice.chat.dto.SendChatMessageRequest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clinical chat (PRD §13.3). Persists the transcript to Postgres, broadcasts each message to the
 * consultation's STOMP topic for live delivery, and emits a PHI-free {@code ChatMessageSent} marker
 * so audit/analytics can count activity without reading the conversation off a topic.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EventPublisher eventPublisher;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            SimpMessagingTemplate messagingTemplate,
            EventPublisher eventPublisher) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> history(String appointmentId) {
        return chatMessageRepository.findByAppointmentIdOrderBySentAtAsc(appointmentId).stream()
                .map(ChatService::toDto)
                .toList();
    }

    /**
     * Persist and broadcast a message. The role is taken from the authenticated caller (passed by
     * the controller) so a physician cannot post as the patient.
     */
    @Transactional
    public ChatMessageDto send(
            String appointmentId, String resolvedSenderRole, String senderRef, SendChatMessageRequest request) {
        Instant now = Instant.now();
        String role = resolvedSenderRole.toUpperCase(Locale.ROOT);
        boolean clinical = request.clinical() == null || request.clinical();

        ChatMessageEntity entity =
                new ChatMessageEntity(
                        IdGenerator.newUlid(),
                        appointmentId,
                        role,
                        senderRef,
                        request.body(),
                        clinical,
                        now);
        chatMessageRepository.save(entity);

        ChatMessageDto dto = toDto(entity);

        // Live fan-out to everyone subscribed to this consultation.
        messagingTemplate.convertAndSend("/topic/consultation/" + appointmentId, dto);

        // PHI-free activity marker (no body) — consistent with the platform chat event contract.
        eventPublisher.publish(
                appointmentId,
                new ChatMessageSent(appointmentId, appointmentId, entity.getId(), role, now));
        log.debug("Chat message {} persisted for {}", entity.getId(), appointmentId);
        return dto;
    }

    private static ChatMessageDto toDto(ChatMessageEntity e) {
        return new ChatMessageDto(
                e.getId(),
                e.getAppointmentId(),
                e.getSenderRole(),
                e.getBody(),
                e.isClinical(),
                DateTimeFormatter.ISO_INSTANT.format(e.getSentAt()));
    }
}
