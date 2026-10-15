package com.telemedicine.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.ProviderOutageException;
import com.telemedicine.common.events.payload.NotificationSent;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.notificationservice.adapter.NotifyChannelAdapter;
import com.telemedicine.notificationservice.adapter.NotifyChannelAdapter.SendResult;
import com.telemedicine.notificationservice.api.dto.SendNotificationRequest;
import com.telemedicine.notificationservice.client.IntegrationAppointmentClient;
import com.telemedicine.notificationservice.config.NotificationServiceProperties;
import com.telemedicine.notificationservice.domain.NotificationLogRepository;
import com.telemedicine.notificationservice.domain.NotificationTemplateEntity;
import com.telemedicine.notificationservice.domain.NotificationTemplateRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationTemplateRepository templateRepository;
    @Mock private NotificationLogRepository logRepository;
    @Mock private NotifyChannelAdapter channelAdapter;
    @Mock private PlatformConfigClient platformConfigClient;
    @Mock private OutboxRecorder outboxRecorder;
    @Mock private IntegrationAppointmentClient appointmentClient;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        NotificationServiceProperties properties = new NotificationServiceProperties();
        properties.setDefaultLocale("es");
        notificationService =
                new NotificationService(
                        templateRepository,
                        logRepository,
                        channelAdapter,
                        platformConfigClient,
                        outboxRecorder,
                        appointmentClient,
                        properties);
        when(platformConfigClient.snapshot()).thenReturn(PlatformConfigSnapshot.defaults());
        when(logRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendUsesEnabledChannelAndEmitsNotificationSent() {
        when(templateRepository.findByTemplateKeyAndLocaleAndChannelAndActiveTrue(
                        "session-link", "es", "sms"))
                .thenReturn(
                        Optional.of(
                                new NotificationTemplateEntity(
                                        "tpl-1",
                                        "session-link",
                                        "es",
                                        "sms",
                                        null,
                                        "Link for {{appointmentId}}",
                                        true)));
        when(channelAdapter.send(eq("sms"), eq("pat-1"), any(), any()))
                .thenReturn(new SendResult(true, "msg-1", null));

        var response =
                notificationService.send(
                        new SendNotificationRequest(
                                "session-link",
                                "es",
                                "sms",
                                "apt-1",
                                "pat-1",
                                Map.of("appointmentId", "apt-1")));

        assertThat(response.status()).isEqualTo("SENT");
        assertThat(response.channel()).isEqualTo("SMS");
        verify(outboxRecorder).record(eq("notification"), any(), any(NotificationSent.class), any());
    }

    @Test
    void sendSkipsDisabledChannelAndFallsBack() {
        when(platformConfigClient.snapshot())
                .thenReturn(
                        new PlatformConfigSnapshot(
                                "simulator",
                                "simulator",
                                true,
                                Map.of("sms", false, "email", true, "whatsapp", false, "push", false),
                                Map.of(),
                                "{}",
                                Instant.now()));
        when(templateRepository.findByTemplateKeyAndLocaleAndChannelAndActiveTrue(
                        "session-link", "es", "email"))
                .thenReturn(
                        Optional.of(
                                new NotificationTemplateEntity(
                                        "tpl-2",
                                        "session-link",
                                        "es",
                                        "email",
                                        "Subject",
                                        "Body {{appointmentId}}",
                                        true)));
        when(channelAdapter.send(eq("email"), eq("pat-1"), any(), any()))
                .thenReturn(new SendResult(true, "msg-2", null));

        var response =
                notificationService.send(
                        new SendNotificationRequest(
                                "session-link",
                                "es",
                                "sms",
                                "apt-1",
                                "pat-1",
                                Map.of("appointmentId", "apt-1")));

        assertThat(response.channel()).isEqualTo("EMAIL");
        verify(channelAdapter, never()).send(eq("sms"), any(), any(), any());
    }

    @Test
    void sendFailsWhenTemplateMissing() {
        when(templateRepository.findByTemplateKeyAndLocaleAndChannelAndActiveTrue(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                notificationService.send(
                                        new SendNotificationRequest(
                                                "missing-template",
                                                "es",
                                                "sms",
                                                "apt-1",
                                                "pat-1",
                                                Map.of())))
                .isInstanceOf(com.telemedicine.common.core.error.DomainException.class)
                .extracting(ex -> ((com.telemedicine.common.core.error.DomainException) ex).getCode())
                .isEqualTo(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);
    }

    @Test
    void sendFailsWhenAllChannelsUnavailable() {
        when(platformConfigClient.snapshot())
                .thenReturn(
                        new PlatformConfigSnapshot(
                                "simulator",
                                "simulator",
                                true,
                                Map.of("sms", false, "email", false, "whatsapp", false, "push", false),
                                Map.of(),
                                "{}",
                                Instant.now()));

        assertThatThrownBy(
                        () ->
                                notificationService.send(
                                        new SendNotificationRequest(
                                                "session-link",
                                                "es",
                                                "sms",
                                                "apt-1",
                                                "pat-1",
                                                Map.of())))
                .isInstanceOf(ProviderOutageException.class)
                .extracting(ex -> ((ProviderOutageException) ex).getCode())
                .isEqualTo(ErrorCode.NOTIFICATION_CHANNEL_UNAVAILABLE);
    }
}
