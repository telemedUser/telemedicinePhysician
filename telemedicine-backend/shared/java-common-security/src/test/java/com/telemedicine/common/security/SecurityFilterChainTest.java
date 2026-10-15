package com.telemedicine.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end behaviour of the auto-configured filter chain in a real servlet context. */
@SpringBootTest(classes = SecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "spring.application.name=waiting-room-service",
            "telemedicine.security.jwt.audiences=telemedicine-api"
        })
class SecurityFilterChainTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("an unauthenticated call is refused with a problem document, not an empty body")
    void unauthenticatedRequestsGetAProblemDocument() throws Exception {
        mockMvc.perform(get("/v1/appointments").header(Headers.CORRELATION_ID, "01JCORR"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().string(Headers.CORRELATION_ID, "01JCORR"))
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.code()))
                .andExpect(jsonPath("$.correlationId").value("01JCORR"))
                .andExpect(jsonPath("$.instance").value("/v1/appointments"));
    }

    @Test
    void authenticatedCallsGetThrough() throws Exception {
        mockMvc.perform(get("/v1/appointments").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("appointments"));
    }

    @Test
    @DisplayName("probes stay reachable so the container can start the service")
    void healthProbesAreOpen() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a role check failure is a problem document reporting insufficient role")
    void deniedRequestsGetAProblemDocument() throws Exception {
        mockMvc.perform(get("/v1/admin-area").with(jwt().authorities(authority(Roles.PATIENT))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INSUFFICIENT_ROLE.code()));
    }

    @Test
    void metaAnnotationsEnforceTheirRole() throws Exception {
        mockMvc.perform(get("/v1/patient-area").with(jwt().authorities(authority(Roles.PATIENT))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/admin-area").with(jwt().authorities(authority(Roles.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("@RequiresRole admits any of its roles and refuses the rest")
    void requiresRoleAdmitsAnyListedRole() throws Exception {
        mockMvc.perform(
                        get("/v1/clinical-notes")
                                .with(jwt().authorities(authority(Roles.PHYSICIAN))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/clinical-notes").with(jwt().authorities(authority(Roles.ADMIN))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/clinical-notes").with(jwt().authorities(authority(Roles.PATIENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the default consent verifier lets the request through, and says so at startup")
    void consentGateIsPermissiveUntilAVerifierExists() throws Exception {
        mockMvc.perform(get("/v1/consultations/APT-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("consultation APT-1"));
    }

    @Test
    @DisplayName("roles arrive through the Keycloak converter, not just through the test harness")
    void keycloakClaimsAreConvertedByTheChain() throws Exception {
        mockMvc.perform(
                        get("/v1/admin-area")
                                .with(
                                        jwt().jwt(
                                                        builder ->
                                                                builder.claim(
                                                                        "realm_access",
                                                                        Map.of(
                                                                                "roles",
                                                                                List.of("ADMIN"))))
                                                .authorities(
                                                        new com.telemedicine.common.security.jwt
                                                                .KeycloakRoleConverter(List.of()))))
                .andExpect(status().isOk());
    }

    @Test
    void securityHeadersAreSentOnEveryResponse() throws Exception {
        // HSTS is only meaningful, and only written, over TLS.
        mockMvc.perform(get("/v1/appointments").secure(true).with(jwt()))
                .andExpect(
                        header().string(
                                        "Content-Security-Policy",
                                        "default-src 'none'; frame-ancestors 'self'"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(
                        header().string(
                                        "Permissions-Policy",
                                        "camera=(self), microphone=(self), geolocation=(),"
                                                + " payment=()"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(
                        header().string(
                                        "Strict-Transport-Security",
                                        "max-age=31536000 ; includeSubDomains"))
                .andExpect(header().doesNotExist("X-Frame-Options"));
    }

    private static org.springframework.security.core.authority.SimpleGrantedAuthority authority(
            String role) {
        return new org.springframework.security.core.authority.SimpleGrantedAuthority(role);
    }
}
