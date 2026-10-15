package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class MdcKafkaConsumerInterceptorBranchTest {

    @Test
    void toleratesMissingHeaders() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L, "key", "value");
        MdcKafkaConsumerInterceptor.applyRecordContext(record);
        assertThat(org.slf4j.MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void ignoresBlankHeaderValues() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L, "key", "value");
        record.headers()
                .add(
                        Headers.CORRELATION_ID,
                        "   ".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MdcKafkaConsumerInterceptor.applyRecordContext(record);
        assertThat(org.slf4j.MDC.get("correlationId")).isNull();
    }
}
