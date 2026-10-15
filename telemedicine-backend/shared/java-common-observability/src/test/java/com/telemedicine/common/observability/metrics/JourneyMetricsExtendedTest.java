package com.telemedicine.common.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class JourneyMetricsExtendedTest {

    @Test
    void recordsVideoEnterTimer() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JourneyMetrics metrics = new JourneyMetrics(registry);
        Timer.Sample sample = metrics.startVideoEnterSample();
        metrics.recordVideoEnterTime(sample, null);
        assertThat(registry.find(JourneyMetrics.METRIC_VIDEO_ENTER_TIME).timer()).isNotNull();
    }
}
