package com.telemedicine.telemetryservice.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.DomainEventListener;
import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.consumer.IdempotentConsumer;
import com.telemedicine.common.events.payload.ConfigurationChanged;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ConfigChangeListener {

    private static final Logger log = LoggerFactory.getLogger(ConfigChangeListener.class);

    private final ObjectMapper eventObjectMapper;
    private final PlatformConfigClient platformConfigClient;

    public ConfigChangeListener(
            @Qualifier("eventObjectMapper") ObjectMapper eventObjectMapper,
            PlatformConfigClient platformConfigClient) {
        this.eventObjectMapper = eventObjectMapper;
        this.platformConfigClient = platformConfigClient;
    }

    @DomainEventListener(EventTypes.CONFIGURATION_CHANGED)
    @IdempotentConsumer
    public void onConfigurationChanged(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment)
            throws Exception {
        EventEnvelope<ConfigurationChanged> envelope =
                readEnvelope(record.value(), ConfigurationChanged.class);
        ConfigurationChanged payload = envelope.payload();
        platformConfigClient.invalidate();
        acknowledgment.acknowledge();
        log.debug("Invalidated telemetry config cache after change to key {}", payload.key());
    }

    private <T> EventEnvelope<T> readEnvelope(byte[] payload, Class<T> payloadType) throws Exception {
        JavaType type =
                eventObjectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        return eventObjectMapper.readValue(payload, type);
    }
}
