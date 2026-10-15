package com.telemedicine.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RolesAndScopesTest {

    @Test
    @DisplayName("prefixing is idempotent, so a caller cannot produce ROLE_ROLE_ADMIN")
    void authorityPrefixingIsIdempotent() {
        assertThat(Roles.authority("ADMIN")).isEqualTo(Roles.ADMIN);
        assertThat(Roles.authority("ROLE_ADMIN")).isEqualTo(Roles.ADMIN);
        assertThat(Roles.authority("  PATIENT  ")).isEqualTo(Roles.PATIENT);
        assertThat(Roles.authority(null)).isNull();
        assertThat(Roles.authority("   ")).isNull();
    }

    @Test
    void realmNamesAndAuthoritiesLineUp() {
        assertThat(Roles.authority(Roles.REALM_PATIENT)).isEqualTo(Roles.PATIENT);
        assertThat(Roles.authority(Roles.REALM_PHYSICIAN)).isEqualTo(Roles.PHYSICIAN);
        assertThat(Roles.authority(Roles.REALM_ADMIN)).isEqualTo(Roles.ADMIN);
        assertThat(Roles.authority(Roles.REALM_SERVICE)).isEqualTo(Roles.SERVICE);
        assertThat(Roles.REALM_ROLES)
                .containsExactly(Roles.REALM_PATIENT, Roles.REALM_PHYSICIAN, Roles.REALM_ADMIN);
    }

    @Test
    void scopeAuthorityPrefixingIsIdempotent() {
        assertThat(Scopes.authority(Scopes.APPOINTMENTS_READ)).isEqualTo("SCOPE_appointments:read");
        assertThat(Scopes.authority("SCOPE_appointments:read"))
                .isEqualTo("SCOPE_appointments:read");
        assertThat(Scopes.authority(null)).isNull();
        assertThat(Scopes.authority(" ")).isNull();
    }

    @Test
    void scopeConstantsAreNamespaced() {
        assertThat(
                        java.util.List.of(
                                Scopes.APPOINTMENTS_READ,
                                Scopes.APPOINTMENTS_WRITE,
                                Scopes.CONSULTATION_JOIN,
                                Scopes.DOCUMENTS_READ,
                                Scopes.DOCUMENTS_ISSUE,
                                Scopes.TELEMETRY_WRITE,
                                Scopes.AUDIT_READ,
                                Scopes.CONFIG_WRITE))
                .allSatisfy(scope -> assertThat(scope).contains(":"));
    }
}
