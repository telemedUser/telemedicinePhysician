package com.telemedicine.identityservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IdentityServiceContractTest {

    @Test
    void contractDocumentsIdentityEndpoints() throws Exception {
        Path contract =
                Path.of(System.getProperty("telemedicine.contracts.dir", ""))
                        .resolve("identity-service.yaml");
        String spec = Files.readString(contract);
        assertThat(spec).contains("/api/v1/identity/validate");
        assertThat(spec).contains("/api/v1/identity/fonasa/2fa/challenge");
        assertThat(spec).contains("/api/v1/identity/fonasa/2fa/verify");
    }
}
