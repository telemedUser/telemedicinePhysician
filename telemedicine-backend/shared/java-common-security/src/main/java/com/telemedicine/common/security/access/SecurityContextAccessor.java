package com.telemedicine.common.security.access;

import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.Scopes;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Reads the authenticated caller out of the security context.
 *
 * <p>Exists so that JPA auditing, the audit aspect and controllers all resolve the actor the same
 * way. Every accessor is null-safe and returns an empty value when there is no authentication —
 * scheduled jobs, Kafka consumers and startup code all run without one, and throwing there would
 * turn a background task into an outage.
 */
public class SecurityContextAccessor {

    private final TelemedicineSecurityProperties.Jwt properties;

    public SecurityContextAccessor() {
        this(new TelemedicineSecurityProperties.Jwt());
    }

    public SecurityContextAccessor(TelemedicineSecurityProperties.Jwt properties) {
        this.properties =
                properties == null ? new TelemedicineSecurityProperties.Jwt() : properties;
    }

    /** The bearer token of the current request, when one was presented. */
    public Optional<Jwt> currentToken() {
        Authentication authentication = authentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Optional.of(jwtAuthentication.getToken());
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    /** Encoded token value, for forwarding to a downstream service. */
    public Optional<String> currentTokenValue() {
        return currentToken().map(Jwt::getTokenValue);
    }

    /** Keycloak subject of the caller. */
    public Optional<String> currentUserId() {
        return currentToken()
                .map(jwt -> jwt.getClaimAsString(properties.getPrincipalClaim()))
                .filter(SecurityContextAccessor::hasText)
                .or(() -> Optional.ofNullable(authenticationName()));
    }

    public Optional<String> currentUsername() {
        return currentToken()
                .map(jwt -> jwt.getClaimAsString("preferred_username"))
                .filter(SecurityContextAccessor::hasText);
    }

    /**
     * Domain patient identifier. Falls back to the subject when the realm maps no dedicated claim,
     * which is the current Phase 0 realm configuration.
     */
    public Optional<String> currentPatientId() {
        return claimOrSubjectForRole(properties.getPatientIdClaim(), Roles.PATIENT);
    }

    /** Domain physician identifier, with the same subject fallback as the patient identifier. */
    public Optional<String> currentPhysicianId() {
        return claimOrSubjectForRole(properties.getPhysicianIdClaim(), Roles.PHYSICIAN);
    }

    /** {@code ROLE_}-prefixed authorities of the caller. */
    public Set<String> currentRoles() {
        return authoritiesWithPrefix(Roles.PREFIX, true);
    }

    /** Scope values of the caller, with the {@code SCOPE_} prefix stripped. */
    public Set<String> currentScopes() {
        return authoritiesWithPrefix(Scopes.PREFIX, false);
    }

    public boolean hasRole(String role) {
        return role != null && currentRoles().contains(Roles.authority(role));
    }

    public boolean isAuthenticated() {
        Authentication authentication = authentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /** Full snapshot of the caller. Never null; returns {@link CurrentUser#anonymous()} instead. */
    public CurrentUser currentUser() {
        if (!isAuthenticated()) {
            return CurrentUser.anonymous();
        }
        return new CurrentUser(
                currentUserId().orElse(null),
                currentUsername().orElse(null),
                currentPatientId().orElse(null),
                currentPhysicianId().orElse(null),
                currentRoles(),
                currentScopes(),
                true);
    }

    private Optional<String> claimOrSubjectForRole(String claimName, String role) {
        Optional<String> fromClaim =
                currentToken()
                        .map(jwt -> jwt.getClaimAsString(claimName))
                        .filter(SecurityContextAccessor::hasText);
        if (fromClaim.isPresent()) {
            return fromClaim;
        }
        return currentRoles().contains(role) ? currentUserId() : Optional.empty();
    }

    private Set<String> authoritiesWithPrefix(String prefix, boolean keepPrefix) {
        Authentication authentication = authentication();
        if (authentication == null) {
            return Set.of();
        }
        Set<String> matches = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith(prefix)) {
                matches.add(keepPrefix ? value : value.substring(prefix.length()));
            }
        }
        return matches;
    }

    private static String authenticationName() {
        Authentication authentication = authentication();
        return authentication == null ? null : authentication.getName();
    }

    private static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
