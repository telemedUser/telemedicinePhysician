package com.telemedicine.common.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

class PhiRedactingMessageConverterBranchTest {

    @Test
    void passesThroughNullAndEmptyMessages() {
        PhiRedactingMessageConverter converter = new PhiRedactingMessageConverter();
        LoggingEvent empty = new LoggingEvent();
        empty.setMessage("");
        assertThat(converter.convert(empty)).isEmpty();

        LoggingEvent nullMessage = new LoggingEvent();
        nullMessage.setMessage(null);
        assertThat(converter.convert(nullMessage)).isNull();
    }
}
