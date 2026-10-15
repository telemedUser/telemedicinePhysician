package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcKafkaProducerInterceptorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void addsHeadersFromMdc() {
        MDC.put(MdcKeys.CORRELATION_ID, "prod-corr");
        MdcKafkaProducerInterceptor interceptor = new MdcKafkaProducerInterceptor();

        ProducerRecord<Object, Object> record =
                interceptor.onSend(new ProducerRecord<>("topic", "key", "value"));

        assertThat(
                        new String(
                                record.headers().lastHeader(Headers.CORRELATION_ID).value(),
                                StandardCharsets.UTF_8))
                .isEqualTo("prod-corr");
    }
}
