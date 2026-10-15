package com.telemedicine.common.observability.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import com.telemedicine.common.core.phi.PhiRedactor;
import java.io.IOException;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;

/** JSON log message provider that redacts PHI before the document is shipped to Elasticsearch. */
public class PhiRedactingMessageJsonProvider extends MessageJsonProvider {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        JsonWritingUtils.writeStringField(
                generator, getFieldName(), PhiRedactor.redact(event.getFormattedMessage()));
    }
}
