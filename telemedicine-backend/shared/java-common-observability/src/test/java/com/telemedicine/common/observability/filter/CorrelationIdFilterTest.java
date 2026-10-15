package com.telemedicine.common.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void echoesInboundCorrelationIdAndPopulatesMdc() throws Exception {
        TelemedicineObservabilityProperties properties = new TelemedicineObservabilityProperties();
        CorrelationIdFilter filter = new CorrelationIdFilter(properties, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Headers.CORRELATION_ID, "corr-inbound");
        request.addHeader(properties.getCorrelation().getAppointmentHeader(), "appt-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringChain = new AtomicReference<>();
        FilterChain chain =
                (ServletRequest req, ServletResponse res) ->
                        mdcDuringChain.set(MDC.get(MdcKeys.CORRELATION_ID));

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(Headers.CORRELATION_ID)).isEqualTo("corr-inbound");
        assertThat(mdcDuringChain.get()).isEqualTo("corr-inbound");
        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
        assertThat(MDC.get(MdcKeys.APPOINTMENT_ID)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenAbsent() throws Exception {
        CorrelationIdFilter filter =
                new CorrelationIdFilter(new TelemedicineObservabilityProperties(), null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(Headers.CORRELATION_ID)).isNotBlank();
    }
}
