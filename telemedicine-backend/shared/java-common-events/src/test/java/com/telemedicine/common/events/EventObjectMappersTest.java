package com.telemedicine.common.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventObjectMappersTest {

    @Test
    void configuresAnExistingMapper() {
        ObjectMapper mapper = new ObjectMapper();
        EventObjectMappers.configure(mapper);
        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    void serialisesInstantsAsIso8601() throws Exception {
        ObjectMapper mapper = EventObjectMappers.create();
        EventEnvelope<String> envelope =
                EventEnvelope.<String>builder()
                        .type(EventTypes.CONFIGURATION_CHANGED)
                        .subject("APT-1")
                        .occurredAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .payload("x")
                        .build();

        String json = mapper.writeValueAsString(envelope);

        assertThat(json).contains("2026-01-01T00:00:00Z");
        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    void ignoresUnknownPropertiesOnDeserialisation() throws Exception {
        ObjectMapper mapper = EventObjectMappers.create();
        String json =
                """
                {"eventId":"1","type":"ConfigurationChanged","version":"1.0",\
                "occurredAt":"2026-01-01T00:00:00Z","subject":"APT-1",\
                "payload":"x","futureField":"ignored"}\
                """;

        EventEnvelope<?> envelope = mapper.readValue(json, EventEnvelope.class);

        assertThat(envelope.type()).isEqualTo(EventTypes.CONFIGURATION_CHANGED);
    }
}
