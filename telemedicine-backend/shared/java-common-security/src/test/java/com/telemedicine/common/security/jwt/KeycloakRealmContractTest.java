package com.telemedicine.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Asserts that this module and the provisioned Keycloak realm describe the same roles and clients.
 *
 * <p>The failure this prevents is silent and total: rename a realm role and every {@code hasRole}
 * check in seventeen services starts returning false, with a valid token, a healthy service and a
 * 403 that looks like an authorisation decision rather than a configuration drift.
 */
class KeycloakRealmContractTest {

    private static JsonNode realm;

    @BeforeAll
    static void loadRealm() throws IOException {
        Path platform = Path.of(System.getProperty("telemedicine.platform.dir", ""));
        Path realmFile = platform.resolve("infra/keycloak/realm-telemedicine.json");
        assertThat(realmFile)
                .withFailMessage(
                        "Expected the Phase 0 Keycloak realm at %s. Set the"
                                + " telemedicine.platform.dir system property if the platform tree"
                                + " has moved.",
                        realmFile)
                .exists();
        realm = new ObjectMapper().readTree(Files.readString(realmFile));
    }

    @Test
    void theRealmIsTheOneThisModuleTargets() {
        assertThat(realm.path("realm").asText()).isEqualTo("telemedicine");
    }

    @Test
    @DisplayName("every role the realm defines has a matching constant, and vice versa")
    void realmRolesAndConstantsAgree() {
        List<String> realmRoles = names(realm.path("roles").path("realm"), "name");

        assertThat(realmRoles).containsExactlyInAnyOrderElementsOf(Roles.REALM_ROLES);
    }

    @Test
    @DisplayName("the default role-client allow-list covers every client the realm provisions")
    void defaultRoleClientsCoverTheRealmClients() {
        List<String> realmClients = names(realm.path("clients"), "clientId");
        List<String> configured = new TelemedicineSecurityProperties().getJwt().getRoleClients();

        assertThat(configured).containsAll(realmClients);
    }

    @Test
    @DisplayName("a token shaped like the realm's own demo users maps to the expected authority")
    void convertsTokensForEachSeededUser() {
        KeycloakRoleConverter converter =
                new KeycloakRoleConverter(
                        new TelemedicineSecurityProperties().getJwt().getRoleClients());

        for (JsonNode user : realm.path("users")) {
            List<String> assignedRoles = new ArrayList<>();
            user.path("realmRoles").forEach(role -> assignedRoles.add(role.asText()));
            Jwt token =
                    Jwt.withTokenValue("token")
                            .header("alg", "RS256")
                            .subject(user.path("username").asText())
                            .claim("preferred_username", user.path("username").asText())
                            .claim("realm_access", Map.of("roles", assignedRoles))
                            .build();

            List<String> expected = assignedRoles.stream().map(Roles::authority).toList();
            assertThat(converter.convert(token))
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrderElementsOf(expected);
        }
    }

    private static List<String> names(JsonNode array, String field) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(node -> node.path(field).asText())
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
