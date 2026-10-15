package com.telemedicine.common.audit;

/** Raised when a hash chain fails integrity verification. */
public class AuditChainTamperException extends RuntimeException {

    public AuditChainTamperException(String message) {
        super(message);
    }
}
