package com.telemedicine.notificationservice.service;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.ProviderOutageException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.NotificationFailed;
import com.telemedicine.common.events.payload.NotificationSent;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.notificationservice.adapter.NotifyChannelAdapter;
import com.telemedicine.notificationservice.adapter.NotifyChannelAdapter.SendResult;
import com.telemedicine.notificationservice.api.dto.AppointmentReminderJobResponse;
import com.telemedicine.notificationservice.api.dto.SendNotificationRequest;
import com.telemedicine.notificationservice.api.dto.SendNotificationResponse;
import com.telemedicine.notificationservice.client.IntegrationAppointmentClient;
import com.telemedicine.notificationservice.client.IntegrationAppointmentClient.ReminderAppointment;
import com.telemedicine.notificationservice.config.NotificationServiceProperties;
import com.telemedicine.notificationservice.domain.NotificationLogEntity;
import com.telemedicine.notificationservice.domain.NotificationLogRepository;
import com.telemedicine.notificationservice.domain.NotificationTemplateEntity;
import com.telemedicine.notificationservice.domain.NotificationTemplateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final List<String> FALLBACK_ORDER = List.of("sms", "email", "whatsapp", "push");
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final NotifyChannelAdapter channelAdapter;
    private final PlatformConfigClient platformConfigClient;
    private final OutboxRecorder outboxRecorder;
    private final IntegrationAppointmentClient appointmentClient;
    private final NotificationServiceProperties properties;

    public NotificationService(
            NotificationTemplateRepository templateRepository,
            NotificationLogRepository logRepository,
            NotifyChannelAdapter channelAdapter,
            PlatformConfigClient platformConfigClient,
            OutboxRecorder outboxRecorder,
            IntegrationAppointmentClient appointmentClient,
            NotificationServiceProperties properties) {
        this.templateRepository = templateRepository;
        this.logRepository = logRepository;
        this.channelAdapter = channelAdapter;
        this.platformConfigClient = platformConfigClient;
        this.outboxRecorder = outboxRecorder;
        this.appointmentClient = appointmentClient;
        this.properties = properties;
    }

    @Transactional
    public SendNotificationResponse send(SendNotificationRequest request) {
        String locale = request.locale() != null ? request.locale() : properties.getDefaultLocale();
        List<String> channels = resolveChannels(request.channel(), platformConfigClient.snapshot());
        if (channels.isEmpty()) {
            throw new ProviderOutageException(
                    ErrorCode.NOTIFICATION_CHANNEL_UNAVAILABLE,
                    request.channel() != null ? request.channel() : "any",
                    "No notification channels are enabled");
        }

        String notificationId = IdGenerator.newUlid();
        Instant now = Instant.now();
        NotificationLogEntity log =
                new NotificationLogEntity(
                        notificationId,
                        request.appointmentId(),
                        channels.getFirst(),
                        request.templateKey(),
                        request.recipientRef(),
                        STATUS_FAILED,
                        0,
                        null,
                        null,
                        now,
                        now);
        logRepository.save(log);

        DomainException lastFailure = null;
        for (String channel : channels) {
            NotificationTemplateEntity template =
                    templateRepository
                            .findByTemplateKeyAndLocaleAndChannelAndActiveTrue(
                                    request.templateKey(), locale, channel)
                            .orElse(null);
            if (template == null) {
                continue;
            }

            String body = render(template.getBody(), request.variables());
            String subject = render(template.getSubject(), request.variables());
            log.setChannel(channel);
            log.setAttempts(log.getAttempts() + 1);
            log.setUpdatedAt(Instant.now());

            try {
                SendResult result = channelAdapter.send(channel, request.recipientRef(), body, subject);
                log.setStatus(STATUS_SENT);
                log.setProviderMessageId(result.messageId());
                log.setUpdatedAt(Instant.now());
                logRepository.save(log);

                outboxRecorder.record(
                        "notification",
                        notificationId,
                        new NotificationSent(
                                notificationId,
                                request.appointmentId(),
                                channel.toUpperCase(Locale.ROOT),
                                request.templateKey(),
                                request.recipientRef(),
                                Instant.now()),
                        correlationId());

                return new SendNotificationResponse(
                        notificationId, channel.toUpperCase(Locale.ROOT), request.templateKey(), STATUS_SENT);
            } catch (ProviderOutageException exception) {
                lastFailure = exception;
                log.setLastErrorCode(exception.getCode().code());
            }
        }

        log.setStatus(STATUS_FAILED);
        log.setUpdatedAt(Instant.now());
        if (lastFailure != null) {
            log.setLastErrorCode(lastFailure.getCode().code());
        } else {
            log.setLastErrorCode(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND.code());
        }
        logRepository.save(log);

        outboxRecorder.record(
                "notification",
                notificationId,
                new NotificationFailed(
                        notificationId,
                        request.appointmentId(),
                        log.getChannel().toUpperCase(Locale.ROOT),
                        request.templateKey(),
                        log.getLastErrorCode(),
                        Instant.now()),
                correlationId());

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new DomainException(
                ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                "No template found for key " + request.templateKey());
    }

    @Transactional
    public AppointmentReminderJobResponse runAppointmentReminders() {
        List<ReminderAppointment> reminders =
                appointmentClient.upcomingReminders(properties.getReminderWindowHours());
        if (reminders.isEmpty()) {
            return new AppointmentReminderJobResponse(0, 0, 0, true);
        }

        int sent = 0;
        int skipped = 0;
        for (ReminderAppointment reminder : reminders) {
            try {
                send(
                        new SendNotificationRequest(
                                "appointment-reminder",
                                properties.getDefaultLocale(),
                                "sms",
                                reminder.appointmentId(),
                                reminder.patientRef(),
                                Map.of("appointmentId", reminder.appointmentId())));
                sent++;
            } catch (RuntimeException exception) {
                skipped++;
            }
        }
        return new AppointmentReminderJobResponse(reminders.size(), sent, skipped, false);
    }

    @Transactional
    public void dispatchEvent(
            String templateKey, String preferredChannel, String appointmentId, String recipientRef, Map<String, String> variables) {
        send(
                new SendNotificationRequest(
                        templateKey,
                        properties.getDefaultLocale(),
                        preferredChannel,
                        appointmentId,
                        recipientRef,
                        variables));
    }

    private static List<String> resolveChannels(String preferred, PlatformConfigSnapshot snapshot) {
        Set<String> ordered = new LinkedHashSet<>();
        if (preferred != null && !preferred.isBlank()) {
            ordered.add(preferred.toLowerCase(Locale.ROOT));
        }
        ordered.addAll(FALLBACK_ORDER);

        List<String> enabled = new ArrayList<>();
        Map<String, Boolean> channelConfig = snapshot.notificationChannels();
        for (String channel : ordered) {
            if (Boolean.TRUE.equals(channelConfig.getOrDefault(channel, true))) {
                enabled.add(channel);
            }
        }
        return enabled;
    }

    private static String render(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        String rendered = template;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return rendered;
    }

    private static String correlationId() {
        String fromMdc = MDC.get(MdcKeys.CORRELATION_ID);
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }
}
