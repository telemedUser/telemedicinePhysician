package com.telemedicine.common.events;

/**
 * Kafka record headers carried alongside every domain event.
 *
 * <p>The {@code ce-*} names follow the CloudEvents binary content mode, so a message can be read by
 * a CloudEvents-aware consumer without unpacking the body, and so infrastructure — a bridge, a
 * schema registry, a dead-letter inspector — can route on type without deserialising the payload.
 *
 * <p>This is also where metadata added after 1.0 goes. {@link EventEnvelope} is a frozen record;
 * adding a header is backwards compatible, adding a component is not.
 */
public final class EventHeaders {

    /** CloudEvents specification version this platform emits. */
    public static final String SPEC_VERSION_VALUE = "1.0";

    public static final String CE_SPEC_VERSION = "ce_specversion";
    public static final String CE_ID = "ce_id";
    public static final String CE_TYPE = "ce_type";
    public static final String CE_SOURCE = "ce_source";
    public static final String CE_SUBJECT = "ce_subject";
    public static final String CE_TIME = "ce_time";
    public static final String CE_DATA_CONTENT_TYPE = "ce_datacontenttype";
    public static final String CE_DATA_SCHEMA = "ce_dataschema";

    /** Journey-wide identifier, matching the HTTP header of the same name. */
    public static final String CORRELATION_ID = "X-Correlation-Id";

    public static final String CAUSATION_ID = "X-Causation-Id";

    /** W3C Trace Context, so a consumer's spans join the producer's trace. */
    public static final String TRACEPARENT = "traceparent";

    public static final String TENANT_ID = "X-Tenant-Id";

    /** Payload schema version, mirroring {@link EventEnvelope#version()}. */
    public static final String SCHEMA_VERSION = "X-Schema-Version";

    public static final String CONTENT_TYPE_JSON = "application/json";

    /** Base URI under which payload schemas are published. */
    public static final String SCHEMA_BASE = "https://telemedicine.bupa/schemas/events/";

    private EventHeaders() {}

    /** Dereferenceable schema URI for an event type and version. */
    public static String schemaUri(String eventType, String version) {
        return SCHEMA_BASE + eventType + "/" + version;
    }
}
