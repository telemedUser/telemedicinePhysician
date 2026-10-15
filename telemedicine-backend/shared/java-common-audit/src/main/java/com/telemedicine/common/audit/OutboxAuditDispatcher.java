package com.telemedicine.common.audit;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.events.payload.AuditRecorded;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Records audit events via the transactional outbox for at-least-once delivery. */
public class OutboxAuditDispatcher implements AuditEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxAuditDispatcher.class);

    private final OutboxRecorder outboxRecorder;

    public OutboxAuditDispatcher(OutboxRecorder outboxRecorder) {
        this.outboxRecorder = outboxRecorder;
    }

    @Override
    public void dispatch(AuditEvent event) {
        AuditRecorded payload =
                new AuditRecorded(
                        event.auditId(),
                        event.actorId(),
                        event.action(),
                        event.resourceType(),
                        event.resourceId(),
                        event.outcomeName(),
                        event.previousHash(),
                        event.hash(),
                        event.occurredAt());
        String correlationId =
                event.correlationId() != null
                        ? event.correlationId()
                        : MDC.get(MdcKeys.CORRELATION_ID);
        outboxRecorder.record("audit", event.auditId(), payload, correlationId, event.auditId());
        log.debug(
                "Queued audit event {} action={} resource={}/{} outcome={}",
                event.auditId(),
                event.action(),
                event.resourceType(),
                event.resourceId(),
                event.outcomeName());
    }
}
