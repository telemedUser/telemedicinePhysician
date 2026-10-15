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

class RecMedAdapterTest {

    private WireMockServer wireMockServer;
    private RecMedAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        SimulatorStubs.recmed(wireMockServer);
        TelemedicineIntegrationProperties properties = new TelemedicineIntegrationProperties();
        properties.getSimulator().setBaseUrl(wireMockServer.baseUrl());
        IntegrationHttpClientFactory clientFactory =
                new IntegrationHttpClientFactory(properties, List.of(), List.of(), null);
        adapter =
                new RecMedAdapter(
                        Mockito.mock(IntegrationFailureReporter.class), clientFactory, properties);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void issuesPrescriptionThroughSimulator() {
        RecMedPrescription prescription =
                adapter.issuePrescription("apt-001", "Paracetamol 500mg");

        assertThat(prescription.prescriptionId()).isNotBlank();
    }
}
