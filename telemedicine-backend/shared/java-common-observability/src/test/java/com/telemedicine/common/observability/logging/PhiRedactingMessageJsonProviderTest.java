package com.telemedicine.common.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringWriter;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import org.junit.jupiter.api.Test;

class PhiRedactingMessageJsonProviderTest {

    @Test
    void redactsEmailInJsonOutput() throws Exception {
        PhiRedactingMessageJsonProvider provider = new PhiRedactingMessageJsonProvider();
        provider.setFieldName("message");

        LoggingEventJsonProviders providers = new LoggingEventJsonProviders();
        providers.addProvider(provider);

        LoggingEvent event = new LoggingEvent();
        event.setMessage("email patient@example.com");
        event.setTimeStamp(System.currentTimeMillis());

        StringWriter writer = new StringWriter();
        com.fasterxml.jackson.core.JsonFactory factory =
                new com.fasterxml.jackson.core.JsonFactory();
        try (com.fasterxml.jackson.core.JsonGenerator generator = factory.createGenerator(writer)) {
            generator.writeStartObject();
            providers.writeTo(generator, event);
            generator.writeEndObject();
        }

        JsonNode json = new ObjectMapper().readTree(writer.toString());
        assertThat(json.get("message").asText()).doesNotContain("patient@example.com");
    }
}
