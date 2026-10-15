package com.telemedicine.common.events.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.TelemedicineEventsProperties;
import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/** Applies {@link IdempotentConsumer} by consulting {@link ProcessedEventStore}. */
@Aspect
public class IdempotentConsumerAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentConsumerAspect.class);

    private final ProcessedEventStore store;
    private final ObjectMapper objectMapper;
    private final TelemedicineEventsProperties properties;
    private final Environment environment;

    public IdempotentConsumerAspect(
            ProcessedEventStore store,
            ObjectMapper objectMapper,
            TelemedicineEventsProperties properties,
            Environment environment) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.environment = environment;
    }

    @SuppressWarnings("checkstyle:IllegalThrows")
    @Around("@annotation(idempotentConsumer)")
    public Object skipDuplicates(
            ProceedingJoinPoint joinPoint, IdempotentConsumer idempotentConsumer) throws Throwable {
        String eventId = extractEventId(joinPoint.getArgs());
        if (eventId == null || eventId.isBlank()) {
            log.warn(
                    "IdempotentConsumer on {} could not resolve an eventId; proceeding without"
                            + " deduplication",
                    joinPoint.getSignature());
            return joinPoint.proceed();
        }

        String group = resolveGroup(idempotentConsumer.group());
        if (store.find(group, eventId).isPresent()) {
            log.info("Skipping duplicate event {} for group {}", eventId, group);
            return null;
        }

        Object result = joinPoint.proceed();
        String eventType = extractEventType(joinPoint.getArgs());
        Duration ttl = properties.getConsumer().getProcessedEventTtl();
        store.markProcessed(group, eventId, eventType, ttl);
        return result;
    }

    private String resolveGroup(String configured) {
        if (configured != null && configured.startsWith("${") && configured.endsWith("}")) {
            String key = configured.substring(2, configured.length() - 1);
            int colon = key.indexOf(':');
            String property = colon >= 0 ? key.substring(0, colon) : key;
            String fallback = colon >= 0 ? key.substring(colon + 1) : "telemedicine";
            return environment.getProperty(property, fallback);
        }
        return configured;
    }

    private String extractEventId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String text && looksLikeEventId(text)) {
                return text;
            }
            if (arg instanceof byte[] bytes) {
                String fromJson = readJsonField(bytes, "eventId");
                if (fromJson != null) {
                    return fromJson;
                }
            }
            if (arg instanceof org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record) {
                String header = header(record, "ce_id");
                if (header != null) {
                    return header;
                }
                if (record.value() instanceof byte[] payload) {
                    return readJsonField(payload, "eventId");
                }
            }
        }
        return null;
    }

    private String extractEventType(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record) {
                String header = header(record, "ce_type");
                if (header != null) {
                    return header;
                }
                if (record.value() instanceof byte[] payload) {
                    return readJsonField(payload, "type");
                }
            }
            if (arg instanceof byte[] bytes) {
                return readJsonField(bytes, "type");
            }
        }
        return null;
    }

    private static String header(
            org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, String name) {
        org.apache.kafka.common.header.Header header = record.headers().lastHeader(name);
        return header == null
                ? null
                : new String(header.value(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private String readJsonField(byte[] json, String field) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asText();
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean looksLikeEventId(String value) {
        return value.length() >= 20
                && value.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '-');
    }
}
