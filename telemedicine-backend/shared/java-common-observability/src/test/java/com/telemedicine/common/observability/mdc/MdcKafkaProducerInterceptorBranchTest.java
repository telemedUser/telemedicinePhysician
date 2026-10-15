package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

class MdcKafkaProducerInterceptorBranchTest {

    @Test
    void skipsBlankMdcValues() {
        MdcKafkaProducerInterceptor interceptor = new MdcKafkaProducerInterceptor();
        ProducerRecord<Object, Object> record =
                interceptor.onSend(new ProducerRecord<>("topic", "key", "value"));
        assertThat(record.headers().lastHeader(Headers.CORRELATION_ID)).isNull();
    }
}
