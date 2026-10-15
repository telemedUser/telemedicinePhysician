package com.telemedicine.common.testing.wiremock;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimulatorStubsIndividualTest {

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void registersHisStubs() {
        SimulatorStubs.his(server);
        assertThat(server.listAllStubMappings().getMappings()).isNotEmpty();
    }

    @Test
    void registersFonasaStubs() {
        SimulatorStubs.fonasa(server);
        assertThat(server.listAllStubMappings().getMappings()).isNotEmpty();
    }

    @Test
    void registersVonageStubs() {
        SimulatorStubs.vonage(server);
        assertThat(server.listAllStubMappings().getMappings()).hasSizeGreaterThan(0);
    }

    @Test
    void deterministicIdsAreStable() {
        assertThat(SimulatorStubs.deterministicId("prefix", "seed"))
                .isEqualTo(SimulatorStubs.deterministicId("prefix", "seed"));
    }
}
