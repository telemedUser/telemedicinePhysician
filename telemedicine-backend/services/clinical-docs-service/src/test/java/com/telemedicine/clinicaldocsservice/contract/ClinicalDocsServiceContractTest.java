package com.telemedicine.clinicaldocsservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ClinicalDocsServiceContractTest {

    @Test
    void contractDocumentsClinicalDocsEndpoints() throws Exception {
        Path contract =
                Path.of(System.getProperty("telemedicine.contracts.dir", ""))
                        .resolve("clinical-docs-service.yaml");
        String spec = Files.readString(contract);
        assertThat(spec).contains("/api/v1/clinical-docs");
        assertThat(spec).contains("/api/v1/clinical-docs/{appointmentId}/prescriptions");
        assertThat(spec).contains("/api/v1/clinical-docs/{appointmentId}/medical-leave");
        assertThat(spec).contains("/api/v1/clinical-docs/{appointmentId}");
    }
}
