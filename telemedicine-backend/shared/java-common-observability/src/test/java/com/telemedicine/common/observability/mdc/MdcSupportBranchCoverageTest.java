package com.telemedicine.common.observability.mdc;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class MdcSupportBranchCoverageTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsAnonymousUserAndBlankOptionalHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("anonymousUser", "n/a"));

        MdcSupport.populateFromRequest(request, new TelemedicineObservabilityProperties(), null);

        assertThat(MDC.get(MdcKeys.USER_ID)).isNull();
        assertThat(MDC.get(MdcKeys.CAUSATION_ID)).isNull();
    }

    @Test
    void populatesTraceIdentifiersWhenTracerPresent() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-1");
        when(traceContext.spanId()).thenReturn("span-1");

        MdcSupport.populateTracing(tracer);

        assertThat(MDC.get(MdcKeys.TRACE_ID)).isEqualTo("trace-1");
        assertThat(MDC.get(MdcKeys.SPAN_ID)).isEqualTo("span-1");
    }

    @Test
    void ignoresNullTracerAndSpan() {
        MdcSupport.populateTracing(null);
        MdcSupport.populateTracing(mock(Tracer.class));
        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
    }
}
