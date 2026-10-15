package com.telemedicine.common.testing.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiContractTestSupport extends OpenApiContractTest {

    @Test
    void loadsSharedErrorContract() throws Exception {
        String yaml = readContract("common-error.yaml");
        assertThat(yaml).contains("ProblemDetail");
        assertThat(validatorFor("common-error.yaml")).isNotNull();
    }
}
