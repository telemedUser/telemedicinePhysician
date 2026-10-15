package com.telemedicine.videoservice.kafka;

import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.payload.ConfigurationChanged;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class ConfigChangeListenerTest {

    @Mock private PlatformConfigClient platformConfigClient;
    @Mock private Acknowledgment acknowledgment;

    private ConfigChangeListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new ConfigChangeListener(objectMapper, platformConfigClient);
    }

    @Test
    void configurationChangedInvalidatesCache() throws Exception {
        EventEnvelope<ConfigurationChanged> envelope =
                EventEnvelope.<ConfigurationChanged>builder()
                        .eventId("evt-1")
                        .type("ConfigurationChanged")
                        .subject("video.provider")
                        .payload(
                                new ConfigurationChanged(
                                        "video.provider",
                                        "GLOBAL",
                                        "abc",
                                        "def",
                                        "admin-1",
                                        Instant.parse("2026-06-01T12:00:00Z")))
                        .build();
        byte[] bytes = objectMapper.writeValueAsBytes(envelope);

        listener.onConfigurationChanged(
                new ConsumerRecord<>("ConfigurationChanged", 0, 0, "video.provider", bytes),
                acknowledgment);

        verify(platformConfigClient).invalidate();
        verify(acknowledgment).acknowledge();
    }
}
