package com.telemedicine.common.audit;

import com.telemedicine.common.core.phi.PhiRedactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fallback dispatcher when outbox is unavailable. */
public class LoggingAuditDispatcher implements AuditEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditDispatcher.class);

    @Override
    public void dispatch(AuditEvent event) {
        log.warn(
                "AUDIT_FALLBACK auditId={} actor={} role={} action={} resource={}/{} outcome={}"
                        + " sequence={} hash={} correlationId={} reason={}",
                event.auditId(),
                event.actorId(),
                event.actorRole(),
                event.action(),
                event.resourceType(),
                PhiRedactor.redact(event.resourceId()),
                event.outcomeName(),
                event.sequence(),
                event.hash(),
                event.correlationId(),
                PhiRedactor.redact(event.reason()));
    }
}
