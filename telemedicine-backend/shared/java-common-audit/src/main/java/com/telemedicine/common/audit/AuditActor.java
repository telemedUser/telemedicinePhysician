package com.telemedicine.common.audit;

/**
 * Who performed an auditable action.
 *
 * @param id stable caller identifier, typically the JWT subject
 * @param role primary realm role at the time of the action
 * @param ip client address when the action originated from an HTTP request
 * @param userAgent user agent header when available
 */
public record AuditActor(String id, String role, String ip, String userAgent) {

    public static AuditActor system(String role) {
        return new AuditActor("system", role, null, null);
    }
}
