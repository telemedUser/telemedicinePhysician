package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

class MdcSupportExtendedTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void populatesOptionalHeadersAndGeneratesMissingOnes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(Headers.CAUSATION_ID, "cause-1");

        MdcSupport.populateFromRequest(request, new TelemedicineObservabilityProperties(), null);

        assertThat(MDC.get(MdcKeys.CAUSATION_ID)).isEqualTo("cause-1");
        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNotBlank();
        assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNotBlank();
    }

    @Test
    void putEventContextHonoursBlankValues() {
        MdcSupport.putEventContext(" ", " ");
        assertThat(MDC.get(MdcKeys.EVENT_ID)).isNull();
        assertThat(MDC.get(MdcKeys.EVENT_TYPE)).isNull();

        MdcSupport.putEventContext("evt", null);
        assertThat(MDC.get(MdcKeys.EVENT_ID)).isEqualTo("evt");
        assertThat(MDC.get(MdcKeys.EVENT_TYPE)).isNull();
    }
}
