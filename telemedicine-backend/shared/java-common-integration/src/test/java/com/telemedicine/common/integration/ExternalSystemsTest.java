package com.telemedicine.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalSystemsTest {

    @Test
    void listsAllElevenSystems() {
        assertThat(ExternalSystems.ALL)
                .containsExactly(
                        ExternalSystems.HIS,
                        ExternalSystems.SAP,
                        ExternalSystems.PAYMENT,
                        ExternalSystems.FONASA,
                        ExternalSystems.MPI,
                        ExternalSystems.ZENDESK,
                        ExternalSystems.VONAGE,
                        ExternalSystems.ACS,
                        ExternalSystems.ANURA,
                        ExternalSystems.RECMED,
                        ExternalSystems.MEDIPASS);
    }
}
