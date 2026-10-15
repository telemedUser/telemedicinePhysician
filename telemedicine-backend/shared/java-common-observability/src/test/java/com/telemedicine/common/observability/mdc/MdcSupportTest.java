package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class MdcSupportTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesRequestScopedKeys() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Headers.CORRELATION_ID, "corr-1");
        request.addHeader(Headers.TENANT_ID, "tenant-a");
        request.addHeader("X-Appointment-Id", "appt-42");

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "user-1",
                                "n/a",
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))));

        MdcSupport.populateFromRequest(request, new TelemedicineObservabilityProperties(), null);

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isEqualTo("corr-1");
        assertThat(MDC.get(MdcKeys.TENANT_ID)).isEqualTo("tenant-a");
        assertThat(MDC.get(MdcKeys.APPOINTMENT_ID)).isEqualTo("appt-42");
        assertThat(MDC.get(MdcKeys.USER_ID)).isEqualTo("user-1");

        MdcSupport.clearRequestScope();
        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
    }

    @Test
    void populatesEventContext() {
        MdcSupport.putEventContext("evt-1", "LinkGenerated");
        assertThat(MDC.get(MdcKeys.EVENT_ID)).isEqualTo("evt-1");
        MdcSupport.clearEventContext();
        assertThat(MDC.get(MdcKeys.EVENT_ID)).isNull();
    }
}
