package com.telemedicine.surveyservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SurveyServiceContractTest {

    @Test
    void contractDocumentsSurveyEndpoints() throws Exception {
        Path contract =
                Path.of(System.getProperty("telemedicine.contracts.dir", ""))
                        .resolve("survey-service.yaml");
        String spec = Files.readString(contract);
        assertThat(spec).contains("/api/v1/survey/{appointmentId}/responses");
        assertThat(spec).contains("/api/v1/survey/{appointmentId}");
        assertThat(spec).contains("/api/v1/internal/jobs/survey-dispatch/run");
    }
}
