package com.telemedicine.common.testing.wiremock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimulatorStubsContractSyncTest {

    @Test
    void hisBodyContainsEnrichedFields() {
        String body = SimulatorResponseBodies.hisAppointmentBody("apt-001");
        assertThat(body).contains("specialty", "physicianId", "facilityCode", "appointmentType");
    }

    @Test
    void paymentBodyContainsEnrichedFields() {
        String body = SimulatorResponseBodies.paymentValidateBody("apt-001", true, "PAID");
        assertThat(body).contains("currency", "amount", "paymentMethod", "paidAt");
    }

    @Test
    void mpiBodyContainsEnrichedFields() {
        String body = SimulatorResponseBodies.mpiPatientBody("patient-001");
        assertThat(body).contains("gender", "bloodType", "insuranceType", "address");
    }

    @Test
    void vonageSessionContainsEnrichedFields() {
        String body = SimulatorResponseBodies.vonageSessionBody();
        assertThat(body).contains("apiKey", "maxParticipants", "expiresAt");
    }

    @Test
    void anuraResultsContainMetricsBlock() {
        String body = SimulatorResponseBodies.anuraResultsBody();
        assertThat(body).contains("measurementId", "confidence", "metrics");
    }
}
