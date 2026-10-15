package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcKafkaConsumerInterceptorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void restoresMdcFromKafkaHeaders() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L, "key", "value");
        record.headers()
                .add(
                        new RecordHeader(
                                Headers.CORRELATION_ID,
                                "kafka-corr".getBytes(StandardCharsets.UTF_8)));
        record.headers()
                .add(
                        new RecordHeader(
                                "ce-type", "AppointmentBooked".getBytes(StandardCharsets.UTF_8)));

        MdcKafkaConsumerInterceptor.applyRecordContext(record);

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isEqualTo("kafka-corr");
        assertThat(MDC.get(MdcKeys.EVENT_TYPE)).isEqualTo("AppointmentBooked");
    }
}
