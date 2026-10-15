package com.telemedicine.common.core.constants;

/**
 * HTTP header names shared across the gateway, every service and the external simulators.
 *
 * <p>These exist as constants because a typo in a correlation header is invisible at compile time
 * and only shows up as a broken trace during an incident.
 */
public final class Headers {

    /** Journey-wide trace identifier. Set by the gateway, echoed by every service. */
    public static final String CORRELATION_ID = "X-Correlation-Id";

    /** Identifier of the message or request that directly caused the current one. */
    public static final String CAUSATION_ID = "X-Causation-Id";

    /** Client-supplied key making an unsafe operation replay-safe. */
    public static final String IDEMPOTENCY_KEY = "X-Idempotency-Key";

    /** Per-request identifier, unique even across retries of the same correlation. */
    public static final String REQUEST_ID = "X-Request-Id";

    /** Reserved for future multi-tenant deployments. */
    public static final String TENANT_ID = "X-Tenant-Id";

    /**
     * Scenario selector honoured by {@code external-simulators} so failure paths can be driven
     * deterministically from tests. Values: {@code success}, {@code failure}, {@code timeout}.
     */
    public static final String SIM_SCENARIO = "X-Sim-Scenario";

    /** W3C Trace Context header, propagated so Jaeger can stitch spans across services. */
    public static final String TRACEPARENT = "traceparent";

    private Headers() {}
}
