package com.telemedicine.common.observability.mdc;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

/** Injects correlation headers from the producer thread MDC into Kafka records. */
public class MdcKafkaProducerInterceptor implements ProducerInterceptor<Object, Object> {

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        putHeader(record, Headers.CORRELATION_ID, MDC.get(MdcKeys.CORRELATION_ID));
        putHeader(record, Headers.CAUSATION_ID, MDC.get(MdcKeys.CAUSATION_ID));
        putHeader(record, Headers.TENANT_ID, MDC.get(MdcKeys.TENANT_ID));
        putHeader(record, Headers.REQUEST_ID, MDC.get(MdcKeys.REQUEST_ID));
        return record;
    }

    private static void putHeader(ProducerRecord<Object, Object> record, String key, String value) {
        if (value != null && !value.isBlank()) {
            record.headers().add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
