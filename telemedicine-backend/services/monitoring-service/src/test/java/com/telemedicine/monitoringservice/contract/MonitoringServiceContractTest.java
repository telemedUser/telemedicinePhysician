package com.telemedicine.monitoringservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MonitoringServiceContractTest {

    @Test
    void contractDocumentsMonitoringEndpoints() throws Exception {
        Path contract =
                Path.of(System.getProperty("telemedicine.contracts.dir", ""))
                        .resolve("monitoring-service.yaml");
        String spec = Files.readString(contract);
        assertThat(spec).contains("/api/v1/monitoring/snapshot");
        assertThat(spec).contains("/api/v1/monitoring/quality");
    }
}
