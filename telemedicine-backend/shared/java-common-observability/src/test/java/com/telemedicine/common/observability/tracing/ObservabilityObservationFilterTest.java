package com.telemedicine.common.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.MdcKeys;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ObservabilityObservationFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void addsCorrelationAsSpanAttribute() {
        MDC.put(MdcKeys.CORRELATION_ID, "obs-corr");
        MDC.put(MdcKeys.APPOINTMENT_ID, "appt-99");

        Observation.Context context = new Observation.Context();
        new ObservabilityObservationFilter().map(context);

        assertThat(context.getLowCardinalityKeyValues())
                .anyMatch(
                        kv ->
                                kv.getKey().equals("correlation.id")
                                        && kv.getValue().equals("obs-corr"));
        assertThat(context.getLowCardinalityKeyValues())
                .anyMatch(
                        kv ->
                                kv.getKey().equals("appointment.id")
                                        && kv.getValue().equals("appt-99"));
    }
}
