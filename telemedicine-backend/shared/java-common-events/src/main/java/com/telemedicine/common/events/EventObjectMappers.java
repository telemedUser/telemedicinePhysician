package com.telemedicine.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * The JSON mapping rules every producer and consumer of a domain event must share.
 *
 * <p>Kept separate from the application's own {@code ObjectMapper}, which a service is free to
 * configure for its API. An event written by one service is read by another built at a different
 * time, so the wire format cannot depend on either service's HTTP preferences.
 */
public final class EventObjectMappers {

    private EventObjectMappers() {}

    /** A mapper configured for the event wire format. */
    public static ObjectMapper create() {
        return configure(new ObjectMapper());
    }

    /** Applies the event wire format to an existing mapper, normally a copy of the app's own. */
    public static ObjectMapper configure(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        // ISO-8601 rather than epoch numbers: a timestamp in a dead-letter record has to be
        // readable by a human at three in the morning.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Forward compatibility. A producer that adds a field must not break consumers that were
        // deployed before it, and in a platform of seventeen services that is the normal case.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
