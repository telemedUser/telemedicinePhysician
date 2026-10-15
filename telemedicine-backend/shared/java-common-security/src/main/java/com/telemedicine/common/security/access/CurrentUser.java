package com.telemedicine.common.security.access;

import java.util.Set;

/**
 * Immutable snapshot of the authenticated caller.
 *
 * <p>Carries identifiers and authorities only. Name, e-mail and RUT are PHI and are deliberately
 * absent: this object is passed into audit records and log context, where those values must not
 * appear.
 *
 * @param userId Keycloak subject, or null when unauthenticated
 * @param username human-readable login name, or null
 * @param patientId domain patient identifier when the caller is a patient
 * @param physicianId domain physician identifier when the caller is a physician
 * @param roles {@code ROLE_}-prefixed authorities
 * @param scopes bare scope values, without the {@code SCOPE_} prefix
 * @param authenticated false for anonymous and for requests handled in dev mode
 */
public record CurrentUser(
        String userId,
        String username,
        String patientId,
        String physicianId,
        Set<String> roles,
        Set<String> scopes,
        boolean authenticated) {

    private static final CurrentUser ANONYMOUS =
            new CurrentUser(null, null, null, null, Set.of(), Set.of(), false);

    public CurrentUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    /** The caller when no authentication is present. Never null, so callers need no null checks. */
    public static CurrentUser anonymous() {
        return ANONYMOUS;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Identifier to record as the actor of an audited action: the subject when authenticated, the
     * literal {@code anonymous} otherwise. Never null, because an audit row without an actor is
     * worse than one naming an anonymous caller.
     */
    public String actorId() {
        return userId == null ? "anonymous" : userId;
    }
}
