package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcKafkaProducerInterceptorExtendedTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void addsAllPresentHeaders() {
        MDC.put(MdcKeys.CAUSATION_ID, "cause-k");
        MDC.put(MdcKeys.TENANT_ID, "tenant-k");
        MDC.put(MdcKeys.REQUEST_ID, "req-k");
        ProducerRecord<Object, Object> record =
                new MdcKafkaProducerInterceptor()
                        .onSend(new ProducerRecord<>("topic", "key", "value"));

        assertThat(
                        new String(
                                record.headers().lastHeader(Headers.CAUSATION_ID).value(),
                                StandardCharsets.UTF_8))
                .isEqualTo("cause-k");
        assertThat(
                        new String(
                                record.headers().lastHeader(Headers.TENANT_ID).value(),
                                StandardCharsets.UTF_8))
                .isEqualTo("tenant-k");
    }
}
