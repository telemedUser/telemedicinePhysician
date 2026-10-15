package com.telemedicine.common.testing.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.security.Roles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@org.springframework.test.context.TestExecutionListeners(
        listeners = WithSecurityContextTestExecutionListener.class,
        mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class WithMockJwtAnnotationsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockPatient
    void patientAnnotationInstallsPatientRole() {
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly(Roles.PATIENT);
    }

    @Test
    @WithMockPhysician
    void physicianAnnotationInstallsPhysicianRole() {
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly(Roles.PHYSICIAN);
    }

    @Test
    @WithMockAdmin
    void adminAnnotationInstallsAdminRole() {
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly(Roles.ADMIN);
    }

    @Test
    void securityContextFactoriesBuildExpectedRoles() {
        assertThat(
                        WithMockJwtSecurityContextFactory.contextFor(TestJwts.patient())
                                .getAuthentication()
                                .getAuthorities())
                .extracting(Object::toString)
                .containsExactly(Roles.PATIENT);
        assertThat(
                        WithMockPhysicianSecurityContextFactory.class
                                .getDeclaredMethods()[0]
                                .getName())
                .isEqualTo("createSecurityContext");
    }
}
