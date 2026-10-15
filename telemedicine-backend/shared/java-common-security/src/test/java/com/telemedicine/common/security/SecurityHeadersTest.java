package com.telemedicine.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

class SecurityHeadersTest {

    @Test
    @DisplayName("the derived policy locks a JSON API down and names the frame-ancestor list")
    void derivesAnApiShapedPolicy() {
        TelemedicineSecurityProperties.Headers headers =
                new TelemedicineSecurityProperties().getHeaders();

        assertThat(headers.resolvedContentSecurityPolicy())
                .isEqualTo("default-src 'none'; frame-ancestors 'self'");

        headers.setFrameAncestors(List.of("'self'", "https://his.example"));
        assertThat(headers.resolvedContentSecurityPolicy())
                .isEqualTo("default-src 'none'; frame-ancestors 'self' https://his.example");

        headers.setFrameAncestors(List.of());
        assertThat(headers.resolvedContentSecurityPolicy())
                .isEqualTo("default-src 'none'; frame-ancestors 'none'");

        headers.setFrameAncestors(null);
        assertThat(headers.resolvedContentSecurityPolicy()).endsWith("frame-ancestors 'none'");
    }

    @Test
    void anExplicitPolicyOverridesTheDerivedOne() {
        TelemedicineSecurityProperties.Headers headers =
                new TelemedicineSecurityProperties().getHeaders();
        headers.setContentSecurityPolicy("default-src 'self'");

        assertThat(headers.resolvedContentSecurityPolicy()).isEqualTo("default-src 'self'");

        headers.setContentSecurityPolicy("   ");
        assertThat(headers.resolvedContentSecurityPolicy()).startsWith("default-src 'none'");
    }

    /**
     * The physician UI runs embedded in the hospital information system, so the deployment must be
     * able to widen frame-ancestors. A blanket X-Frame-Options would make that impossible.
     */
    @Nested
    @SpringBootTest(classes = SecurityTestApplication.class)
    @AutoConfigureMockMvc
    @TestPropertySource(
            properties = {
                "telemedicine.security.headers.frame-ancestors='self',https://his.bupa.cl",
                "telemedicine.security.headers.hsts-enabled=false",
                "telemedicine.security.headers.referrer-policy=no-referrer"
            })
    class WithHisEmbedding {

        @Autowired private MockMvc mockMvc;

        @Test
        void allowsTheConfiguredEmbedder() throws Exception {
            mockMvc.perform(get("/v1/appointments").secure(true).with(jwt()))
                    .andExpect(
                            header().string(
                                            "Content-Security-Policy",
                                            "default-src 'none'; frame-ancestors 'self'"
                                                    + " https://his.bupa.cl"))
                    .andExpect(header().string("Referrer-Policy", "no-referrer"))
                    .andExpect(header().doesNotExist("Strict-Transport-Security"));
        }
    }

    @Nested
    @SpringBootTest(classes = SecurityTestApplication.class)
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "telemedicine.security.headers.enabled=false")
    class WithHeadersDisabled {

        @Autowired private MockMvc mockMvc;

        @Test
        @DisplayName("a service fronted by a gateway that sets its own headers can opt out")
        void writesNoSecurityHeaders() throws Exception {
            mockMvc.perform(get("/v1/appointments").secure(true).with(jwt()))
                    .andExpect(header().doesNotExist("Content-Security-Policy"))
                    .andExpect(header().doesNotExist("X-Content-Type-Options"))
                    .andExpect(header().doesNotExist("Permissions-Policy"));
        }
    }
}
