package com.telemedicine.common.security.jwt;

import com.telemedicine.common.security.Roles;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns Keycloak's nested role claims into Spring Security authorities.
 *
 * <p>Keycloak publishes roles in two places and Spring Security reads neither by default, which is
 * why an unmapped resource server authenticates a user successfully and then denies every {@code
 * hasRole} check:
 *
 * <ul>
 *   <li>{@code realm_access.roles} — realm-wide roles, where {@code PATIENT}, {@code PHYSICIAN} and
 *       {@code ADMIN} live in this platform's realm.
 *   <li>{@code resource_access.<client>.roles} — per-client roles, read only for the clients named
 *       in configuration so that a role granted on an unrelated client cannot widen access here.
 * </ul>
 *
 * <p>A service-account token ({@code clientId} present, no username) additionally receives {@link
 * Roles#SERVICE}, giving machine-to-machine callers an authority to authorise against without
 * minting a realm role for every service.
 *
 * <p>Scopes from the {@code scope} claim are emitted as {@code SCOPE_} authorities, matching what
 * Spring Security's own converter would have produced.
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String SCOPE = "scope";
    private static final String CLIENT_ID = "clientId";
    private static final String PREFERRED_USERNAME = "preferred_username";

    /**
     * Keycloak grants these to every user. They describe account self-service, not platform access,
     * so promoting them to authorities would only add noise to every authorisation decision.
     */
    private static final Set<String> IGNORED_REALM_ROLES =
            Set.of("offline_access", "uma_authorization", "default-roles-telemedicine");

    private final List<String> roleClients;

    /** Reads realm roles only. */
    public KeycloakRoleConverter() {
        this(List.of());
    }

    /**
     * @param roleClients {@code resource_access} clients whose roles are honoured; null is treated
     *     as empty
     */
    public KeycloakRoleConverter(List<String> roleClients) {
        this.roleClients = roleClients == null ? List.of() : List.copyOf(roleClients);
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        realmRoles(jwt).forEach(role -> addRole(authorities, role));
        clientRoles(jwt).forEach(role -> addRole(authorities, role));
        if (isServiceAccount(jwt)) {
            authorities.add(new SimpleGrantedAuthority(Roles.SERVICE));
        }
        scopes(jwt).forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
        return authorities;
    }

    private static void addRole(Set<GrantedAuthority> authorities, String role) {
        if (role == null || role.isBlank() || IGNORED_REALM_ROLES.contains(role)) {
            return;
        }
        authorities.add(new SimpleGrantedAuthority(Roles.authority(role.toUpperCase(Locale.ROOT))));
    }

    private static Collection<String> realmRoles(Jwt jwt) {
        return rolesIn(jwt.getClaimAsMap(REALM_ACCESS));
    }

    private Collection<String> clientRoles(Jwt jwt) {
        if (roleClients.isEmpty()) {
            return List.of();
        }
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (resourceAccess == null) {
            return List.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        for (String client : roleClients) {
            Object entry = resourceAccess.get(client);
            if (entry instanceof Map<?, ?> map) {
                roles.addAll(rolesIn(map));
            }
        }
        return roles;
    }

    private static Collection<String> rolesIn(Map<?, ?> claim) {
        if (claim == null) {
            return List.of();
        }
        Object roles = claim.get(ROLES);
        if (!(roles instanceof Collection<?> collection)) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (Object role : collection) {
            if (role != null) {
                names.add(role.toString());
            }
        }
        return names;
    }

    private static Collection<String> scopes(Jwt jwt) {
        String scope = jwt.getClaimAsString(SCOPE);
        if (scope == null || scope.isBlank()) {
            return List.of();
        }
        return List.of(scope.trim().split("\\s+"));
    }

    private static boolean isServiceAccount(Jwt jwt) {
        return jwt.getClaimAsString(CLIENT_ID) != null
                && jwt.getClaimAsString(PREFERRED_USERNAME) == null;
    }
}
