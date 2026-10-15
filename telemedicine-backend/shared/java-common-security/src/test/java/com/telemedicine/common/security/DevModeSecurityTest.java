package com.telemedicine.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** The development bypass, on a profile where it is allowed. */
@SpringBootTest(classes = SecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {"telemedicine.security.mode=DEV", "spring.profiles.active=local"})
class DevModeSecurityTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("dev mode lets an unauthenticated request reach the controller")
    void everythingIsPermitted() throws Exception {
        mockMvc.perform(get("/v1/appointments"))
                .andExpect(status().isOk())
                .andExpect(content().string("appointments"));
    }

    @Test
    @DisplayName("the security headers are still applied, so header changes are testable locally")
    void headersAreStillWritten() throws Exception {
        mockMvc.perform(get("/v1/appointments"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                                .string("X-Content-Type-Options", "nosniff"));
    }
}
