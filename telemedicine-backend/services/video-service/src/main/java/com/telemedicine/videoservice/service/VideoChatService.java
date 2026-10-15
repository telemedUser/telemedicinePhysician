package com.telemedicine.videoservice.service;

import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.ChatMessageSent;
import com.telemedicine.common.integration.provider.ProviderSelector;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.videoservice.adapter.ChatProviderAdapter;
import com.telemedicine.videoservice.domain.ChatMessageEntity;
import com.telemedicine.videoservice.domain.ChatThreadEntity;
import com.telemedicine.videoservice.domain.VideoSessionEntity;
import com.telemedicine.videoservice.repository.ChatMessageRepository;
import com.telemedicine.videoservice.repository.ChatThreadRepository;
import com.telemedicine.videoservice.repository.VideoSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoChatService {

    private final VideoSessionRepository sessionRepository;
    private final ChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final ProviderSelector<Object> chatProviderSelector;
    private final OutboxRecorder outboxRecorder;

    public VideoChatService(
            VideoSessionRepository sessionRepository,
            ChatThreadRepository threadRepository,
            ChatMessageRepository messageRepository,
            @Qualifier("chatProviderSelector") ProviderSelector<Object> chatProviderSelector,
            OutboxRecorder outboxRecorder) {
        this.sessionRepository = sessionRepository;
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.chatProviderSelector = chatProviderSelector;
        this.outboxRecorder = outboxRecorder;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMessages(String sessionId) {
        requireSession(sessionId);
        return messageRepository.findBySessionIdOrderBySentAtAsc(sessionId).stream()
                .map(
                        message ->
                                Map.<String, Object>of(
                                        "messageId", message.getId(),
                                        "sessionId", message.getSessionId(),
                                        "senderRole", message.getSenderRole(),
                                        "body", message.getBody(),
                                        "sentAt", message.getSentAt().toString()))
                .toList();
    }

    @Transactional
    public Map<String, Object> sendMessage(String sessionId, String senderRole, String body) {
        VideoSessionEntity session = requireSession(sessionId);
        ChatThreadEntity thread =
                threadRepository
                        .findBySessionId(sessionId)
                        .orElseGet(() -> createThread(session));

        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(IdGenerator.newUlid());
        message.setThreadId(thread.getId());
        message.setSessionId(sessionId);
        message.setAppointmentId(session.getAppointmentId());
        message.setSenderRole(senderRole);
        message.setBody(body);
        message.setSentAt(Instant.now());
        messageRepository.save(message);

        outboxRecorder.record(
                "ChatMessage",
                message.getId(),
                new ChatMessageSent(
                        session.getAppointmentId(),
                        sessionId,
                        message.getId(),
                        senderRole,
                        message.getSentAt()),
                correlationId(),
                session.getAppointmentId());

        return Map.of(
                "messageId", message.getId(),
                "sessionId", sessionId,
                "senderRole", senderRole,
                "sentAt", message.getSentAt().toString());
    }

    @Transactional
    public ChatThreadEntity ensureThread(VideoSessionEntity session) {
        return threadRepository
                .findBySessionId(session.getId())
                .orElseGet(() -> createThread(session));
    }

    private ChatThreadEntity createThread(VideoSessionEntity session) {
        ChatProviderAdapter chatProvider = activeChatProvider();
        ChatThreadEntity thread = new ChatThreadEntity();
        thread.setId(IdGenerator.newUlid());
        thread.setSessionId(session.getId());
        thread.setAppointmentId(session.getAppointmentId());
        thread.setProvider(chatProvider.providerName());
        return threadRepository.save(thread);
    }

    private ChatProviderAdapter activeChatProvider() {
        return (ChatProviderAdapter) chatProviderSelector.active();
    }

    private VideoSessionEntity requireSession(String sessionId) {
        return sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Video session not found: " + sessionId));
    }

    private static String correlationId() {
        String fromMdc = MDC.get("correlationId");
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }
}
