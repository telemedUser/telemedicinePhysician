package com.telemedicine.physicianservice.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operational configuration for the physician service. Market behaviour (minimum completion set,
 * staleness tolerance) resolves through config-service at runtime; these are the local defaults
 * used when the configuration service is unavailable (TM-INT-008 degraded mode).
 */
@ConfigurationProperties(prefix = "telemedicine.physician")
public class PhysicianServiceProperties {

    private String defaultLocale = "es";
    private String integrationServiceBaseUrl = "http://integration-service:8107";

    /** How long a provider projection may serve before it is marked stale (TM-ARC-020). */
    private int projectionStalenessSeconds = 900;

    /** Target for propagating a waiting-room change to the worklist (TM-NFR-003, TM-WLT-006). */
    private int worklistPropagationSlaSeconds = 3;

    /** Document types that must be present before {@code ConsultationClosed} is emitted (TM-DOC-009). */
    private List<String> minimumCompletionSet = List.of("NOTE", "DIAGNOSIS");

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getIntegrationServiceBaseUrl() {
        return integrationServiceBaseUrl;
    }

    public void setIntegrationServiceBaseUrl(String integrationServiceBaseUrl) {
        this.integrationServiceBaseUrl = integrationServiceBaseUrl;
    }

    public int getProjectionStalenessSeconds() {
        return projectionStalenessSeconds;
    }

    public void setProjectionStalenessSeconds(int projectionStalenessSeconds) {
        this.projectionStalenessSeconds = projectionStalenessSeconds;
    }

    public int getWorklistPropagationSlaSeconds() {
        return worklistPropagationSlaSeconds;
    }

    public void setWorklistPropagationSlaSeconds(int worklistPropagationSlaSeconds) {
        this.worklistPropagationSlaSeconds = worklistPropagationSlaSeconds;
    }

    public List<String> getMinimumCompletionSet() {
        return minimumCompletionSet;
    }

    public void setMinimumCompletionSet(List<String> minimumCompletionSet) {
        this.minimumCompletionSet = minimumCompletionSet;
    }
}
