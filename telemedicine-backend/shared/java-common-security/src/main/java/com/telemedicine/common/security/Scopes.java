package com.telemedicine.common.security;

/**
 * OAuth2 scopes used for coarse-grained capability checks between services.
 *
 * <p>Roles answer "who is calling"; scopes answer "what was this token issued to do". A physician
 * token driving the clinical UI and a physician token issued to a reporting job carry the same role
 * but should not carry the same scopes.
 */
public final class Scopes {

    /** Prefix Spring Security's {@code JwtGrantedAuthoritiesConverter} applies to scopes. */
    public static final String PREFIX = "SCOPE_";

    public static final String APPOINTMENTS_READ = "appointments:read";
    public static final String APPOINTMENTS_WRITE = "appointments:write";
    public static final String CONSULTATION_JOIN = "consultation:join";
    public static final String DOCUMENTS_READ = "documents:read";
    public static final String DOCUMENTS_ISSUE = "documents:issue";
    public static final String TELEMETRY_WRITE = "telemetry:write";
    public static final String AUDIT_READ = "audit:read";
    public static final String CONFIG_WRITE = "config:write";

    private Scopes() {}

    /** Authority form of a scope, for use in {@code hasAuthority(...)} expressions. */
    public static String authority(String scope) {
        if (scope == null || scope.isBlank()) {
            return null;
        }
        String trimmed = scope.trim();
        return trimmed.startsWith(PREFIX) ? trimmed : PREFIX + trimmed;
    }
}
