package com.telemedicine.common.audit;

/** Dispatches audit events to durable storage (outbox) or logging fallback. */
public interface AuditEventDispatcher {

    void dispatch(AuditEvent event);
}
