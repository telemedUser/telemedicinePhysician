package com.telemedicine.common.security;

import com.telemedicine.common.security.annotation.IsAdmin;
import com.telemedicine.common.security.annotation.IsPatient;
import com.telemedicine.common.security.annotation.RequiresRole;
import com.telemedicine.common.security.consent.RequiresConsent;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal application for the servlet-level security tests.
 *
 * <p>Component scanning is deliberately absent: the point of these tests is that the shared beans
 * arrive through auto-configuration alone, exactly as they do in a real service.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class SecurityTestApplication {

    // The controller is a member class of a configuration class, so Spring registers it without a
    // component scan and without an explicit @Bean method.
    @RestController
    public static class ProtectedController {

        @GetMapping("/v1/appointments")
        public String list() {
            return "appointments";
        }

        @GetMapping("/actuator/health")
        public String health() {
            return "UP";
        }

        @GetMapping("/v1/patient-area")
        @IsPatient
        public String patientArea() {
            return "patient";
        }

        @GetMapping("/v1/admin-area")
        @IsAdmin
        public String adminArea() {
            return "admin";
        }

        @GetMapping("/v1/clinical-notes")
        @RequiresRole({"PHYSICIAN", "ADMIN"})
        public String clinicalNotes() {
            return "notes";
        }

        @GetMapping("/v1/consultations/{appointmentId}")
        @RequiresConsent(value = "CLINICAL_INFORMED", subject = "#appointmentId")
        public String consultation(@PathVariable String appointmentId) {
            return "consultation " + appointmentId;
        }
    }
}
