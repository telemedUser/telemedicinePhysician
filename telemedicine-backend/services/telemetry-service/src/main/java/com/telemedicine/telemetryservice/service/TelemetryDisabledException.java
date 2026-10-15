package com.telemedicine.telemetryservice.service;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.ForbiddenException;

public final class TelemetryDisabledException extends ForbiddenException {

    private TelemetryDisabledException() {
        super(ErrorCode.TELEMETRY_NOT_ENABLED, "Telemetry is not enabled for this consultation");
    }

    public static TelemetryDisabledException notEnabled() {
        return new TelemetryDisabledException();
    }
}
