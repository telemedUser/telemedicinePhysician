package com.telemedicine.common.security;

import java.util.List;

/**
 * Authorities granted by {@link com.telemedicine.common.security.jwt.KeycloakRoleConverter}.
 *
 * <p>Two vocabularies meet here. Keycloak stores bare role names ({@code PATIENT}); Spring Security
 * expects the {@code ROLE_} prefix. Both are published so that realm-facing code and
 * {@code @PreAuthorize} expressions can each use the right one instead of hand-concatenating a
 * prefix, which is the usual source of a silently unenforced rule.
 *
 * <p>The realm names must stay in step with {@code
 * telemedicine-platform/infra/keycloak/realm-telemedicine.json}; a test asserts they do.
 */
public final class Roles {

    public static final String PATIENT = "ROLE_PATIENT";
    public static final String PHYSICIAN = "ROLE_PHYSICIAN";
    public static final String ADMIN = "ROLE_ADMIN";

    /** Machine-to-machine callers authenticating with a Keycloak service account. */
    public static final String SERVICE = "ROLE_SERVICE";

    /** Prefix Spring Security requires on a role authority. */
    public static final String PREFIX = "ROLE_";

    /** Realm role names as provisioned in Keycloak, without the Spring prefix. */
    public static final String REALM_PATIENT = "PATIENT";

    public static final String REALM_PHYSICIAN = "PHYSICIAN";
    public static final String REALM_ADMIN = "ADMIN";
    public static final String REALM_SERVICE = "SERVICE";

    /** Every realm role the platform expects Keycloak to define. */
    public static final List<String> REALM_ROLES =
            List.of(REALM_PATIENT, REALM_PHYSICIAN, REALM_ADMIN);

    private Roles() {}

    /** Prefixes a bare realm role, leaving an already-prefixed authority untouched. */
    public static String authority(String realmRole) {
        if (realmRole == null || realmRole.isBlank()) {
            return null;
        }
        String trimmed = realmRole.trim();
        return trimmed.startsWith(PREFIX) ? trimmed : PREFIX + trimmed;
    }
}
