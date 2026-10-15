package com.telemedicine.common.observability.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

class ObservabilityObservationFilterBranchTest {

    @Test
    void ignoresBlankMdcValues() {
        Observation.Context context = new Observation.Context();
        new ObservabilityObservationFilter().map(context);
        assertThat(context.getLowCardinalityKeyValues()).isEmpty();
    }
}
