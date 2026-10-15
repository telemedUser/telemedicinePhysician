package com.telemedicine.common.core.error;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An external enterprise system (HIS, SAP, payment, FONASA, MPI, Zendesk) rejected or failed a
 * call. Always carries the logical system name so alerting and the {@code IntegrationFailed} event
 * can attribute the failure.
 */
public class IntegrationException extends DomainException {

    private final String system;

    public IntegrationException(String system, String message) {
        this(ErrorCode.INTEGRATION_ERROR, system, message, null);
    }

    public IntegrationException(String system, String message, Throwable cause) {
        this(ErrorCode.INTEGRATION_ERROR, system, message, cause);
    }

    public IntegrationException(ErrorCode code, String system, String message) {
        this(code, system, message, null);
    }

    public IntegrationException(ErrorCode code, String system, String message, Throwable cause) {
        super(code, message, detailsFor(system), cause);
        this.system = system;
    }

    private static Map<String, Object> detailsFor(String system) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("system", system);
        return details;
    }

    /** Logical name of the external system, for example {@code his} or {@code fonasa}. */
    public String getSystem() {
        return system;
    }

    public static IntegrationException timeout(String system) {
        return new IntegrationException(
                ErrorCode.EXTERNAL_TIMEOUT, system, system + " did not respond in time", null);
    }

    public static IntegrationException circuitOpen(String system) {
        return new IntegrationException(
                ErrorCode.CIRCUIT_OPEN, system, "Circuit breaker is open for " + system, null);
    }
}
