package com.telemedicine.clinicaldocsservice.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.common.testing.wiremock.SimulatorStubs;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MedipassAdapterTest {

    private WireMockServer wireMockServer;
    private MedipassAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        SimulatorStubs.medipass(wireMockServer);
        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();
        properties.getSimulator().setBaseUrl(wireMockServer.baseUrl());
        IntegrationHttpClientFactory clientFactory =
                new IntegrationHttpClientFactory(properties, List.of(), List.of(), null);
        adapter =
                new MedipassAdapter(
                        Mockito.mock(IntegrationFailureReporter.class), clientFactory, properties);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void issuesCertificateThroughSimulator() {
        MedipassCertificate certificate =
                adapter.issueCertificate("apt-001", 3, "Respiratory infection");

        assertThat(certificate.certificateId()).isNotBlank();
    }
}
