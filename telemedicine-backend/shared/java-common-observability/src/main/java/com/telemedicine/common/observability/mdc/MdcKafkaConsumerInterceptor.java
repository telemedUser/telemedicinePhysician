package com.telemedicine.common.observability.mdc;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

/** Restores MDC from Kafka record headers before listener invocation. */
public class MdcKafkaConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {}

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}

    /** Called by the consumer thread before each record; exposed for unit tests. */
    public static void applyRecordContext(ConsumerRecord<?, ?> record) {
        MdcSupport.clearRequestScope();
        putFromHeader(record, Headers.CORRELATION_ID, MdcKeys.CORRELATION_ID);
        putFromHeader(record, Headers.CAUSATION_ID, MdcKeys.CAUSATION_ID);
        putFromHeader(record, Headers.TENANT_ID, MdcKeys.TENANT_ID);
        putFromHeader(record, Headers.REQUEST_ID, MdcKeys.REQUEST_ID);
        putFromHeader(record, "ce-id", MdcKeys.EVENT_ID);
        putFromHeader(record, "ce-type", MdcKeys.EVENT_TYPE);
    }

    private static void putFromHeader(ConsumerRecord<?, ?> record, String header, String mdcKey) {
        Header kafkaHeader = record.headers().lastHeader(header);
        if (kafkaHeader == null) {
            return;
        }
        String value = new String(kafkaHeader.value(), StandardCharsets.UTF_8);
        if (!value.isBlank()) {
            MDC.put(mdcKey, value);
        }
    }
}
