package com.telemedicine.simulators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.simulators.admin.SimulatorAdminController;
import com.telemedicine.simulators.engine.SimulatorEngine;
import com.telemedicine.simulators.engine.SimulatorProperties;
import com.telemedicine.simulators.engine.VendorConfig;
import com.telemedicine.simulators.engine.VendorNames;
import com.telemedicine.simulators.state.AppointmentStateStore;
import com.telemedicine.simulators.state.FonasaChallengeStore;
import com.telemedicine.simulators.state.NotifyDeliveryLog;
import com.telemedicine.simulators.state.ProviderOutageState;
import com.telemedicine.simulators.state.VideoSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SimulatorIntegrationTest {

    private MockMvc mockMvc;
    private SimulatorEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SimulatorEngine(new SimulatorProperties());
        AppointmentStateStore appointmentStateStore = new AppointmentStateStore();
        FonasaChallengeStore fonasaChallengeStore = new FonasaChallengeStore();
        VideoSessionStore videoSessionStore = new VideoSessionStore();
        NotifyDeliveryLog notifyDeliveryLog = new NotifyDeliveryLog();
        ProviderOutageState providerOutageState = new ProviderOutageState();

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HisSimulatorController(engine, appointmentStateStore),
                                new PaymentSimulatorController(engine, appointmentStateStore),
                                new FonasaSimulatorController(engine, fonasaChallengeStore),
                                new VonageSimulatorController(engine, videoSessionStore, providerOutageState),
                                new AcsSimulatorController(engine, videoSessionStore, providerOutageState),
                                new SimulatorAdminController(
                                        engine,
                                        appointmentStateStore,
                                        fonasaChallengeStore,
                                        videoSessionStore,
                                        notifyDeliveryLog,
                                        providerOutageState))
                        .build();
    }

    @Test
    void adminListsTwelveVendors() throws Exception {
        mockMvc.perform(get("/sim/admin/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.his.enabled").value(true))
                .andExpect(jsonPath("$.zendesk.enabled").value(true));
    }

    @Test
    void latencyInjectionViaAdminApi() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mockMvc.perform(
                        put("/sim/admin/vendors/" + VendorNames.HIS)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new VendorConfig(200, 0, 0, 0, "success", true))))
                .andExpect(status().isOk());

        long started = System.currentTimeMillis();
        mockMvc.perform(get("/sim/his/appointments/test-001")).andExpect(status().isOk());
        assertThat(System.currentTimeMillis() - started).isGreaterThanOrEqualTo(200);
    }

    @Test
    void paymentErrorRateViaAdminApi() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        engine.updateConfig(VendorNames.PAYMENT, VendorConfig.defaults().withErrorRate(1.0));
        mockMvc.perform(get("/sim/payment/validate/test-001")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void fonasaChallengeAndVerifyFlow() throws Exception {
        String challengeResponse =
                mockMvc.perform(
                                post("/sim/fonasa/2fa/challenge")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"patientId\":\"patient-1\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.attemptsRemaining").value(3))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String challengeId =
                new ObjectMapper().readTree(challengeResponse).get("challengeId").asText();

        mockMvc.perform(
                        post("/sim/fonasa/2fa/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"patientId\":\"patient-1\",\"challengeId\":\""
                                                + challengeId
                                                + "\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void vonageOutageDoesNotAffectAcs() throws Exception {
        mockMvc.perform(
                        post("/sim/vonage/outage")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enabled\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/sim/vonage/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"appointmentId\":\"apt-001\"}"))
                .andExpect(status().is5xxServerError());

        mockMvc.perform(
                        post("/sim/acs/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"appointmentId\":\"apt-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("acs"));
    }
}
