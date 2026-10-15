package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MdcContextFactoryTest {

    @Test
    void ofNullOrEmptyReturnsEmptyContext() {
        assertThat(MdcContext.of(null).values()).isEmpty();
        assertThat(MdcContext.of(java.util.Map.of()).values()).isEmpty();
    }
}
