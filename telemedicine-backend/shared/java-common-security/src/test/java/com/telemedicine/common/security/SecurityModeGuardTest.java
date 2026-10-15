package com.telemedicine.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityModeGuardTest {

    @Test
    @DisplayName(
            "the authentication bypass on a production profile stops the service from starting")
    void refusesDevModeOnAProtectedProfile() {
        assertThatIllegalStateException()
                .isThrownBy(() -> guard(SecurityMode.DEV, "prod").afterPropertiesSet())
                .withMessageContaining("telemedicine.security.mode=DEV")
                .withMessageContaining("[prod]");
    }

    @Test
    void namesEveryOffendingProfile() {
        assertThatIllegalStateException()
                .isThrownBy(() -> guard(SecurityMode.DEV, "prod", "staging").afterPropertiesSet())
                .withMessageContaining("[prod, staging]");
    }

    @Test
    void allowsDevModeOnADevelopmentProfile() {
        assertThatCode(() -> guard(SecurityMode.DEV, "local", "mock").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void allowsDevModeWhenNoProfileIsActive() {
        assertThatCode(() -> guard(SecurityMode.DEV).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void neverInterferesWithJwtMode() {
        assertThatCode(() -> guard(SecurityMode.JWT, "prod").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("clearing the protected-profile list is an explicit, deliberate opt-out")
    void honoursAnEmptiedProtectedProfileList() {
        TelemedicineSecurityProperties properties = new TelemedicineSecurityProperties();
        properties.setMode(SecurityMode.DEV);
        properties.setProtectedProfiles(List.of());

        assertThatCode(() -> SecurityModeGuard.verify(properties, Set.of("prod")))
                .doesNotThrowAnyException();

        properties.setProtectedProfiles(null);
        assertThatCode(() -> SecurityModeGuard.verify(properties, Set.of("prod")))
                .doesNotThrowAnyException();
    }

    @Test
    void defaultsCoverTheUsualProductionProfileNames() {
        assertThat(new TelemedicineSecurityProperties().getProtectedProfiles())
                .containsExactly("prod", "production", "staging");
    }

    private static SecurityModeGuard guard(SecurityMode mode, String... activeProfiles) {
        TelemedicineSecurityProperties properties = new TelemedicineSecurityProperties();
        properties.setMode(mode);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);
        return new SecurityModeGuard(properties, environment);
    }
}
