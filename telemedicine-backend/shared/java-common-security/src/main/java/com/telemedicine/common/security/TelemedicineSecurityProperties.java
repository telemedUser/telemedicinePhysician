package com.telemedicine.common.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the shared resource server, role mapping, headers and consent gate. */
@ConfigurationProperties(prefix = "telemedicine.security")
public class TelemedicineSecurityProperties {

    /** Set to false to keep the library on the classpath but register nothing. */
    private boolean enabled = true;

    /** JWT in every real deployment; DEV bypasses authentication entirely. */
    private SecurityMode mode = SecurityMode.JWT;

    /**
     * Profiles on which {@link SecurityMode#DEV} is a startup failure rather than a warning. A
     * permissive filter chain reaching production is the highest-severity failure this module can
     * have, so it is refused rather than logged.
     */
    private List<String> protectedProfiles =
            new ArrayList<>(List.of("prod", "production", "staging"));

    /** Paths served without authentication. Kept narrow: probes, metrics and API documentation. */
    private List<String> permitAllPaths =
            new ArrayList<>(
                    List.of(
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info",
                            "/actuator/prometheus",
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/swagger-ui/**"));

    private final Jwt jwt = new Jwt();
    private final Headers headers = new Headers();
    private final Consent consent = new Consent();
    private final Propagation propagation = new Propagation();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SecurityMode getMode() {
        return mode;
    }

    public void setMode(SecurityMode mode) {
        this.mode = mode;
    }

    public List<String> getProtectedProfiles() {
        return protectedProfiles;
    }

    public void setProtectedProfiles(List<String> protectedProfiles) {
        this.protectedProfiles = protectedProfiles;
    }

    public List<String> getPermitAllPaths() {
        return permitAllPaths;
    }

    public void setPermitAllPaths(List<String> permitAllPaths) {
        this.permitAllPaths = permitAllPaths;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Headers getHeaders() {
        return headers;
    }

    public Consent getConsent() {
        return consent;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    /** Token validation and claim mapping. */
    public static class Jwt {

        /** Defaults to the realm provisioned by the Phase 0 compose stack. */
        private String jwkSetUri =
                "http://keycloak:8080/realms/telemedicine/protocol/openid-connect/certs";

        /**
         * Expected {@code iss}. Validated only when set, because the value differs between the
         * in-cluster hostname and the browser-facing one.
         */
        private String issuer;

        /** Expected {@code aud} values, any one of which satisfies the check. */
        private List<String> audiences = new ArrayList<>();

        /** Tolerance for clock drift between Keycloak and the service. */
        private Duration clockSkew = Duration.ofSeconds(30);

        /** Claim used as the authentication name. */
        private String principalClaim = "sub";

        /** Keycloak clients whose {@code resource_access} roles are honoured. */
        private List<String> roleClients =
                new ArrayList<>(
                        List.of("patient-portal", "physician-ui", "admin-console", "gateway"));

        /** Claim carrying the domain patient identifier, when the realm maps one. */
        private String patientIdClaim = "patient_id";

        /** Claim carrying the domain physician identifier, when the realm maps one. */
        private String physicianIdClaim = "physician_id";

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public List<String> getAudiences() {
            return audiences;
        }

        public void setAudiences(List<String> audiences) {
            this.audiences = audiences;
        }

        public Duration getClockSkew() {
            return clockSkew;
        }

        public void setClockSkew(Duration clockSkew) {
            this.clockSkew = clockSkew;
        }

        public String getPrincipalClaim() {
            return principalClaim;
        }

        public void setPrincipalClaim(String principalClaim) {
            this.principalClaim = principalClaim;
        }

        public List<String> getRoleClients() {
            return roleClients;
        }

        public void setRoleClients(List<String> roleClients) {
            this.roleClients = roleClients;
        }

        public String getPatientIdClaim() {
            return patientIdClaim;
        }

        public void setPatientIdClaim(String patientIdClaim) {
            this.patientIdClaim = patientIdClaim;
        }

        public String getPhysicianIdClaim() {
            return physicianIdClaim;
        }

        public void setPhysicianIdClaim(String physicianIdClaim) {
            this.physicianIdClaim = physicianIdClaim;
        }
    }

    /** Response security headers. */
    public static class Headers {

        private boolean enabled = true;

        private boolean hstsEnabled = true;

        private Duration hstsMaxAge = Duration.ofDays(365);

        /**
         * CSP {@code frame-ancestors} allow-list. The physician UI is embedded in the hospital
         * information system in picture-in-picture mode, so a blanket {@code X-Frame-Options: DENY}
         * would break a stated requirement. Widen this per deployment instead.
         */
        private List<String> frameAncestors = new ArrayList<>(List.of("'self'"));

        /** Full CSP override. When null the policy is derived from {@link #frameAncestors}. */
        private String contentSecurityPolicy;

        private String referrerPolicy = "strict-origin-when-cross-origin";

        /**
         * Camera and microphone stay enabled for same-origin: the product is video consultation.
         */
        private String permissionsPolicy =
                "camera=(self), microphone=(self), geolocation=(), payment=()";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isHstsEnabled() {
            return hstsEnabled;
        }

        public void setHstsEnabled(boolean hstsEnabled) {
            this.hstsEnabled = hstsEnabled;
        }

        public Duration getHstsMaxAge() {
            return hstsMaxAge;
        }

        public void setHstsMaxAge(Duration hstsMaxAge) {
            this.hstsMaxAge = hstsMaxAge;
        }

        public List<String> getFrameAncestors() {
            return frameAncestors;
        }

        public void setFrameAncestors(List<String> frameAncestors) {
            this.frameAncestors = frameAncestors;
        }

        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public String getReferrerPolicy() {
            return referrerPolicy;
        }

        public void setReferrerPolicy(String referrerPolicy) {
            this.referrerPolicy = referrerPolicy;
        }

        public String getPermissionsPolicy() {
            return permissionsPolicy;
        }

        public void setPermissionsPolicy(String permissionsPolicy) {
            this.permissionsPolicy = permissionsPolicy;
        }

        /** The policy actually sent: the override when present, otherwise an API-shaped default. */
        public String resolvedContentSecurityPolicy() {
            if (contentSecurityPolicy != null && !contentSecurityPolicy.isBlank()) {
                return contentSecurityPolicy;
            }
            String ancestors =
                    frameAncestors == null || frameAncestors.isEmpty()
                            ? "'none'"
                            : String.join(" ", frameAncestors);
            return "default-src 'none'; frame-ancestors " + ancestors;
        }
    }

    /** The {@code @RequiresConsent} gate. */
    public static class Consent {

        /** Disabling this makes {@code @RequiresConsent} a no-op; intended for tests only. */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** Outbound header forwarding for service-to-service calls. */
    public static class Propagation {

        private boolean enabled = true;

        /** Forward the caller's bearer token. Disable where downstream uses its own credentials. */
        private boolean forwardAuthorization = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isForwardAuthorization() {
            return forwardAuthorization;
        }

        public void setForwardAuthorization(boolean forwardAuthorization) {
            this.forwardAuthorization = forwardAuthorization;
        }
    }
}
