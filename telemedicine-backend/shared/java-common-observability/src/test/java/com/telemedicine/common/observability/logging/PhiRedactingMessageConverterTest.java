package com.telemedicine.common.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PhiRedactingMessageConverterTest {

    @Test
    void redactsEmailInLogMessage() {
        PhiRedactingMessageConverter converter = new PhiRedactingMessageConverter();
        Logger logger = (Logger) LoggerFactory.getLogger("test");
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName(logger.getName());
        event.setMessage("contact patient@example.com now");
        event.setTimeStamp(System.currentTimeMillis());

        String converted = converter.convert(event);

        assertThat(converted).doesNotContain("patient@example.com");
        assertThat(converted).contains("***");
    }
}
