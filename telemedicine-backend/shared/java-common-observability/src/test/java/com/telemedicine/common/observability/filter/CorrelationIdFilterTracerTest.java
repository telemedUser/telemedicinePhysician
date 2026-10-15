package com.telemedicine.common.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTracerTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void includesTraceIdsWhenTracerAvailable() throws Exception {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-x");
        when(traceContext.spanId()).thenReturn("span-x");

        CorrelationIdFilter filter =
                new CorrelationIdFilter(new TelemedicineObservabilityProperties(), tracer);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    assertThat(MDC.get(MdcKeys.TRACE_ID)).isEqualTo("trace-x");
                    assertThat(MDC.get(MdcKeys.SPAN_ID)).isEqualTo("span-x");
                });
    }
}
