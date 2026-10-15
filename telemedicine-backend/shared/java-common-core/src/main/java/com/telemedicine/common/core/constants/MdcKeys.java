package com.telemedicine.common.core.constants;

/**
 * SLF4J MDC keys. These become top-level fields in the JSON log documents shipped to Elasticsearch,
 * so renaming one breaks saved queries and dashboards.
 */
public final class MdcKeys {

    public static final String CORRELATION_ID = "correlationId";
    public static final String CAUSATION_ID = "causationId";
    public static final String REQUEST_ID = "requestId";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String USER_ID = "userId";
    public static final String USER_ROLE = "userRole";
    public static final String TENANT_ID = "tenantId";
    public static final String SERVICE = "service";

    /** Present on every log line emitted while handling a specific appointment. */
    public static final String APPOINTMENT_ID = "appointmentId";

    /** Present while handling a specific video or telemetry session. */
    public static final String SESSION_ID = "sessionId";

    /** Event identifier, set by the Kafka consumer interceptor. */
    public static final String EVENT_ID = "eventId";

    public static final String EVENT_TYPE = "eventType";

    private MdcKeys() {}
}
