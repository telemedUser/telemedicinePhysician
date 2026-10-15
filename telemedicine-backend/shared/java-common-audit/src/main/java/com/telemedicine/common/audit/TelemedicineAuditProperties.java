package com.telemedicine.common.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for declarative auditing and consent ledger helpers. */
@ConfigurationProperties(prefix = "telemedicine.audit")
public class TelemedicineAuditProperties {

    private boolean enabled = true;

    /** Default actor id when no security context is present. */
    private String systemActorId = "system";

    /** Default actor role when no security context is present. */
    private String systemActorRole = "SYSTEM";

    /**
     * When true, argument digests and reasons are redacted unless {@link Audited#phiSafe()} is
     * false.
     */
    private boolean phiSafeByDefault = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSystemActorId() {
        return systemActorId;
    }

    public void setSystemActorId(String systemActorId) {
        this.systemActorId = systemActorId;
    }

    public String getSystemActorRole() {
        return systemActorRole;
    }

    public void setSystemActorRole(String systemActorRole) {
        this.systemActorRole = systemActorRole;
    }

    public boolean isPhiSafeByDefault() {
        return phiSafeByDefault;
    }

    public void setPhiSafeByDefault(boolean phiSafeByDefault) {
        this.phiSafeByDefault = phiSafeByDefault;
    }
}
