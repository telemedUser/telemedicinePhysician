package com.telemedicine.common.observability.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.telemedicine.common.core.phi.PhiRedactor;

/** Logback message converter that redacts PHI before JSON encoding or console output. */
public class PhiRedactingMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String formatted = super.convert(event);
        if (formatted == null || formatted.isEmpty()) {
            return formatted;
        }
        return PhiRedactor.redact(formatted);
    }
}
