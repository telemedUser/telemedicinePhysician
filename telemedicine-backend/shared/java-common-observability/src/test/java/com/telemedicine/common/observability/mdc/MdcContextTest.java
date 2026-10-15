package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void capturesAndRestoresContext() throws Exception {
        MDC.put(MdcKeys.CORRELATION_ID, "before");
        MdcContext captured = MdcContext.capture();

        MDC.put(MdcKeys.CORRELATION_ID, "during");
        AutoCloseable scope = captured.scope();
        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isEqualTo("before");
        scope.close();

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isEqualTo("during");
    }

    @Test
    void emptyContextIsSafe() {
        MdcContext empty = MdcContext.capture();
        empty.apply();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
