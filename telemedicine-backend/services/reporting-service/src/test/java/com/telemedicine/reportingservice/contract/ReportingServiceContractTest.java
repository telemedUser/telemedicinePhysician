package com.telemedicine.reportingservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReportingServiceContractTest {

    @Test
    void contractDocumentsReportingEndpoints() throws Exception {
        Path contract =
                Path.of(System.getProperty("telemedicine.contracts.dir", ""))
                        .resolve("reporting-service.yaml");
        String spec = Files.readString(contract);
        assertThat(spec).contains("/api/v1/reporting/kpis");
        assertThat(spec).contains("/api/v1/reporting/kpis/{family}");
        assertThat(spec).contains("/api/v1/reporting/exports");
        assertThat(spec).contains("/api/v1/internal/jobs/report-generation/run");
    }
}
