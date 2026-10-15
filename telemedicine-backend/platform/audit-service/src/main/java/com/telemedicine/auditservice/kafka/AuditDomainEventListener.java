package com.telemedicine.auditservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.auditservice.service.AuditService;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.AuditRecorded;
import com.telemedicine.common.events.payload.ChatMessageSent;
import com.telemedicine.common.events.payload.ConsentGranted;
import com.telemedicine.common.events.payload.ConsentWithdrawn;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class AuditDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditDomainEventListener.class);

    private final ObjectMapper eventObjectMapper;
    private final AuditService auditService;

    public AuditDomainEventListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper, AuditService auditService) {
        this.eventObjectMapper = eventObjectMapper;
        this.auditService = auditService;
    }

    @DomainEventListener(EventTypes.AUDIT_RECORDED)
    @IdempotentConsumer
    public void onAuditRecorded(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<AuditRecorded> envelope = readEnvelope(record.value(), AuditRecorded.class);
        auditService.ingestRecorded(envelope.payload(), envelope.correlationId());
        acknowledgment.acknowledge();
        log.debug("Persisted AuditRecorded {}", envelope.payload().auditId());
    }

    @DomainEventListener(EventTypes.CONSENT_GRANTED)
    @IdempotentConsumer
    public void onConsentGranted(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<ConsentGranted> envelope = readEnvelope(record.value(), ConsentGranted.class);
        auditService.recordConsentGranted(envelope.payload(), envelope.subject());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.CONSENT_WITHDRAWN)
    @IdempotentConsumer
    public void onConsentWithdrawn(
            ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws Exception {
        EventEnvelope<ConsentWithdrawn> envelope = readEnvelope(record.value(), ConsentWithdrawn.class);
        auditService.recordConsentWithdrawn(envelope.payload(), envelope.subject());
        acknowledgment.acknowledge();
    }

    @DomainEventListener(EventTypes.CHAT_MESSAGE_SENT)
    @IdempotentConsumer
    public void onChatMessageSent(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<ChatMessageSent> envelope = readEnvelope(record.value(), ChatMessageSent.class);
        auditService.recordChatMessageSent(envelope.payload(), envelope.correlationId());
        acknowledgment.acknowledge();
        log.debug("Recorded chat activity audit for appointment {}", envelope.payload().appointmentId());
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper
                        .getTypeFactory()
                        .constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
