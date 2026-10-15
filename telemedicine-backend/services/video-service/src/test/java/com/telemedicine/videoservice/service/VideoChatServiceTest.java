package com.telemedicine.videoservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.events.payload.ChatMessageSent;
import com.telemedicine.common.integration.provider.ProviderSelector;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.videoservice.domain.ChatMessageEntity;
import com.telemedicine.videoservice.domain.ChatThreadEntity;
import com.telemedicine.videoservice.domain.VideoSessionEntity;
import com.telemedicine.videoservice.repository.ChatMessageRepository;
import com.telemedicine.videoservice.repository.ChatThreadRepository;
import com.telemedicine.videoservice.repository.VideoSessionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoChatServiceTest {

    @Mock private VideoSessionRepository sessionRepository;
    @Mock private ChatThreadRepository threadRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private ProviderSelector<Object> chatProviderSelector;
    @Mock private OutboxRecorder outboxRecorder;

    private VideoChatService chatService;

    @BeforeEach
    void setUp() {
        chatService =
                new VideoChatService(
                        sessionRepository,
                        threadRepository,
                        messageRepository,
                        chatProviderSelector,
                        outboxRecorder);
    }

    @Test
    void publishesPhiFreeChatMessageSentEvent() {
        VideoSessionEntity session = new VideoSessionEntity();
        session.setId("sess-1");
        session.setAppointmentId("apt-1");
        when(sessionRepository.findById("sess-1")).thenReturn(Optional.of(session));

        ChatThreadEntity thread = new ChatThreadEntity();
        thread.setId("thread-1");
        thread.setSessionId("sess-1");
        when(threadRepository.findBySessionId("sess-1")).thenReturn(Optional.of(thread));
        when(messageRepository.save(any(ChatMessageEntity.class)))
                .thenAnswer(
                        invocation -> {
                            ChatMessageEntity saved = invocation.getArgument(0);
                            saved.setSentAt(Instant.parse("2026-01-01T10:00:00Z"));
                            return saved;
                        });

        chatService.sendMessage("sess-1", "PHYSICIAN", "Clinical note");

        ArgumentCaptor<ChatMessageSent> payload = ArgumentCaptor.forClass(ChatMessageSent.class);
        verify(outboxRecorder)
                .record(eq("ChatMessage"), any(), payload.capture(), any(), eq("apt-1"));
        assertThat(payload.getValue().appointmentId()).isEqualTo("apt-1");
        assertThat(payload.getValue().sessionId()).isEqualTo("sess-1");
        assertThat(payload.getValue().senderRole()).isEqualTo("PHYSICIAN");
        assertThat(ChatMessageSent.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("body");
    }
}
