package com.telemedicine.common.security.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityContextAccessorTest {

    private final SecurityContextAccessor accessor =
            new SecurityContextAccessor(new TelemedicineSecurityProperties().getJwt());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName(
            "nothing throws when there is no authentication, because background threads have none")
    void isNullSafeWhenUnauthenticated() {
        assertThat(accessor.isAuthenticated()).isFalse();
        assertThat(accessor.currentToken()).isEmpty();
        assertThat(accessor.currentTokenValue()).isEmpty();
        assertThat(accessor.currentUserId()).isEmpty();
        assertThat(accessor.currentUsername()).isEmpty();
        assertThat(accessor.currentPatientId()).isEmpty();
        assertThat(accessor.currentPhysicianId()).isEmpty();
        assertThat(accessor.currentRoles()).isEmpty();
        assertThat(accessor.currentScopes()).isEmpty();
        assertThat(accessor.hasRole(Roles.ADMIN)).isFalse();
        assertThat(accessor.currentUser()).isEqualTo(CurrentUser.anonymous());
    }

    @Test
    void treatsAnonymousAuthenticationAsUnauthenticated() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new AnonymousAuthenticationToken(
                                "key",
                                "anonymousUser",
                                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(accessor.isAuthenticated()).isFalse();
        assertThat(accessor.currentUser().authenticated()).isFalse();
    }

    @Test
    void readsIdentityRolesAndScopesFromTheToken() {
        authenticate(
                builder -> builder.claim("preferred_username", "physician.demo"),
                Roles.PHYSICIAN,
                "SCOPE_documents:issue");

        assertThat(accessor.currentUserId()).contains("2b3c4d5e");
        assertThat(accessor.currentUsername()).contains("physician.demo");
        assertThat(accessor.currentRoles()).containsExactly(Roles.PHYSICIAN);
        assertThat(accessor.currentScopes()).containsExactly("documents:issue");
        assertThat(accessor.hasRole("PHYSICIAN")).isTrue();
        assertThat(accessor.hasRole(Roles.PHYSICIAN)).isTrue();
        assertThat(accessor.hasRole(Roles.ADMIN)).isFalse();
        assertThat(accessor.hasRole(null)).isFalse();
        assertThat(accessor.currentTokenValue()).contains("token-value");
    }

    @Test
    void prefersADedicatedDomainIdentifierClaimWhenTheRealmMapsOne() {
        authenticate(builder -> builder.claim("patient_id", "PAT-9001"), Roles.PATIENT);

        assertThat(accessor.currentPatientId()).contains("PAT-9001");
    }

    @Test
    @DisplayName("without a mapped claim the subject stands in, but only for the matching role")
    void fallsBackToTheSubjectForTheCallersOwnRole() {
        authenticate(builder -> {}, Roles.PATIENT);

        assertThat(accessor.currentPatientId()).contains("2b3c4d5e");
        assertThat(accessor.currentPhysicianId()).isEmpty();
    }

    @Test
    void ignoresABlankClaimValue() {
        authenticate(builder -> builder.claim("physician_id", "  "), Roles.PHYSICIAN);

        assertThat(accessor.currentPhysicianId()).contains("2b3c4d5e");
    }

    @Test
    void buildsAFullSnapshotOfTheCaller() {
        authenticate(
                builder ->
                        builder.claim("preferred_username", "patient.demo")
                                .claim("patient_id", "PAT-1"),
                Roles.PATIENT,
                "SCOPE_appointments:read");

        CurrentUser user = accessor.currentUser();

        assertThat(user.authenticated()).isTrue();
        assertThat(user.userId()).isEqualTo("2b3c4d5e");
        assertThat(user.username()).isEqualTo("patient.demo");
        assertThat(user.patientId()).isEqualTo("PAT-1");
        assertThat(user.physicianId()).isNull();
        assertThat(user.roles()).containsExactly(Roles.PATIENT);
        assertThat(user.scopes()).containsExactly("appointments:read");
        assertThat(user.actorId()).isEqualTo("2b3c4d5e");
        assertThat(user.hasRole(Roles.PATIENT)).isTrue();
    }

    @Test
    @DisplayName("a non-JWT authentication still yields a user id, so audit rows are never blank")
    void supportsNonJwtAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "operator",
                                "n/a",
                                List.of(new SimpleGrantedAuthority(Roles.ADMIN))));

        assertThat(accessor.currentUserId()).contains("operator");
        assertThat(accessor.currentRoles()).containsExactly(Roles.ADMIN);
        assertThat(accessor.currentToken()).isEmpty();
    }

    @Test
    void findsTheTokenWhenItIsOnlyThePrincipal() {
        Jwt jwt = token(builder -> {});
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(jwt, "n/a", List.of()));

        assertThat(accessor.currentToken()).contains(jwt);
    }

    @Test
    void defaultsItsClaimNamesWhenConstructedWithoutConfiguration() {
        SecurityContextAccessor plain = new SecurityContextAccessor();
        SecurityContextAccessor nullConfigured = new SecurityContextAccessor(null);
        authenticate(builder -> builder.claim("patient_id", "PAT-7"), Roles.PATIENT);

        assertThat(plain.currentPatientId()).contains("PAT-7");
        assertThat(nullConfigured.currentPatientId()).contains("PAT-7");
    }

    private void authenticate(Consumer<Jwt.Builder> customizer, String... authorities) {
        Jwt jwt = token(customizer);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                jwt,
                                List.of(authorities).stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .toList()));
    }

    private static Jwt token(Consumer<Jwt.Builder> customizer) {
        Jwt.Builder builder =
                Jwt.withTokenValue("token-value")
                        .header("alg", "RS256")
                        .subject("2b3c4d5e")
                        .claims(claims -> claims.putAll(Map.of()));
        customizer.accept(builder);
        return builder.build();
    }
}
