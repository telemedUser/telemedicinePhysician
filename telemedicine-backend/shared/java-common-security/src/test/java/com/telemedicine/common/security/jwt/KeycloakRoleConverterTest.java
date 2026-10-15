package com.telemedicine.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.security.Roles;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter =
            new KeycloakRoleConverter(List.of("patient-portal", "physician-ui"));

    @Test
    @DisplayName("realm roles become ROLE_ authorities, which is what hasRole checks against")
    void mapsRealmRoles() {
        Jwt jwt =
                jwt(builder -> builder.claim("realm_access", Map.of("roles", List.of("PATIENT"))));

        assertThat(authorities(jwt)).containsExactly(Roles.PATIENT);
    }

    @Test
    void mapsRolesFromAnAllowedClient() {
        Jwt jwt =
                jwt(
                        builder ->
                                builder.claim(
                                        "resource_access",
                                        Map.of(
                                                "physician-ui",
                                                Map.of("roles", List.of("PHYSICIAN")))));

        assertThat(authorities(jwt)).containsExactly(Roles.PHYSICIAN);
    }

    @Test
    @DisplayName("a role granted on an unlisted client cannot widen access to this service")
    void ignoresRolesFromAnUnlistedClient() {
        Jwt jwt =
                jwt(
                        builder ->
                                builder.claim(
                                        "resource_access",
                                        Map.of(
                                                "some-other-app",
                                                Map.of("roles", List.of("ADMIN")))));

        assertThat(authorities(jwt)).isEmpty();
    }

    @Test
    void combinesRealmAndClientRolesWithoutDuplicates() {
        Jwt jwt =
                jwt(
                        builder ->
                                builder.claim("realm_access", Map.of("roles", List.of("PHYSICIAN")))
                                        .claim(
                                                "resource_access",
                                                Map.of(
                                                        "physician-ui",
                                                        Map.of(
                                                                "roles",
                                                                List.of("PHYSICIAN", "ADMIN")))));

        assertThat(authorities(jwt)).containsExactlyInAnyOrder(Roles.PHYSICIAN, Roles.ADMIN);
    }

    @Test
    @DisplayName("Keycloak's account-management roles are not promoted to platform authorities")
    void dropsKeycloakHousekeepingRoles() {
        Jwt jwt =
                jwt(
                        builder ->
                                builder.claim(
                                        "realm_access",
                                        Map.of(
                                                "roles",
                                                List.of(
                                                        "offline_access",
                                                        "uma_authorization",
                                                        "default-roles-telemedicine",
                                                        "ADMIN"))));

        assertThat(authorities(jwt)).containsExactly(Roles.ADMIN);
    }

    @Test
    void normalisesRoleCase() {
        Jwt jwt =
                jwt(builder -> builder.claim("realm_access", Map.of("roles", List.of("patient"))));

        assertThat(authorities(jwt)).containsExactly(Roles.PATIENT);
    }

    @Test
    @DisplayName("a service-account token gains ROLE_SERVICE without a realm role being minted")
    void grantsServiceRoleToClientCredentialsTokens() {
        Jwt jwt = jwt(builder -> builder.claim("clientId", "gateway"));

        assertThat(authorities(jwt)).containsExactly(Roles.SERVICE);
    }

    @Test
    void doesNotTreatAUserTokenAsAServiceAccount() {
        Jwt jwt =
                jwt(
                        builder ->
                                builder.claim("clientId", "patient-portal")
                                        .claim("preferred_username", "patient.demo"));

        assertThat(authorities(jwt)).doesNotContain(Roles.SERVICE);
    }

    @Test
    void emitsScopesAsScopeAuthorities() {
        Jwt jwt = jwt(builder -> builder.claim("scope", "appointments:read  documents:read"));

        assertThat(authorities(jwt))
                .containsExactly("SCOPE_appointments:read", "SCOPE_documents:read");
    }

    @Test
    void toleratesMissingMalformedAndEmptyClaims() {
        assertThat(authorities(jwt(builder -> {}))).isEmpty();
        assertThat(authorities(jwt(builder -> builder.claim("realm_access", Map.of())))).isEmpty();
        assertThat(
                        authorities(
                                jwt(
                                        builder ->
                                                builder.claim(
                                                        "realm_access",
                                                        Map.of("roles", "PATIENT")))))
                .isEmpty();
        assertThat(
                        authorities(
                                jwt(
                                        builder ->
                                                builder.claim(
                                                        "resource_access",
                                                        Map.of("physician-ui", "not-a-map")))))
                .isEmpty();
        assertThat(authorities(jwt(builder -> builder.claim("scope", "   ")))).isEmpty();
    }

    @Test
    void readsOnlyRealmRolesWhenNoClientsAreConfigured() {
        Jwt jwt =
                jwt(
                        builder ->
                                builder.claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                        .claim(
                                                "resource_access",
                                                Map.of(
                                                        "physician-ui",
                                                        Map.of("roles", List.of("PHYSICIAN")))));

        assertThat(new KeycloakRoleConverter().convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Roles.ADMIN);
        assertThat(new KeycloakRoleConverter(null).convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Roles.ADMIN);
    }

    private List<String> authorities(Jwt jwt) {
        return converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static Jwt jwt(Consumer<Jwt.Builder> customizer) {
        Jwt.Builder builder =
                Jwt.withTokenValue("token")
                        .header("alg", "RS256")
                        .subject("2b3c4d5e")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300));
        customizer.accept(builder);
        return builder.build();
    }
}
