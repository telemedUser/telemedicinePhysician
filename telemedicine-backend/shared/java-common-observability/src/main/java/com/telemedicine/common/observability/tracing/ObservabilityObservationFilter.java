package com.telemedicine.common.observability.tracing;

import com.telemedicine.common.core.constants.MdcKeys;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.slf4j.MDC;

/** Adds correlation and appointment identifiers as span attributes for Jaeger search. */
public class ObservabilityObservationFilter implements ObservationFilter {

    @Override
    public Observation.Context map(Observation.Context context) {
        putIfPresent(context, "correlation.id", MDC.get(MdcKeys.CORRELATION_ID));
        putIfPresent(context, "appointment.id", MDC.get(MdcKeys.APPOINTMENT_ID));
        putIfPresent(context, "user.id", MDC.get(MdcKeys.USER_ID));
        putIfPresent(context, "tenant.id", MDC.get(MdcKeys.TENANT_ID));
        return context;
    }

    private static void putIfPresent(Observation.Context context, String key, String value) {
        if (value != null && !value.isBlank()) {
            context.addLowCardinalityKeyValue(KeyValue.of(key, value));
        }
    }
}
