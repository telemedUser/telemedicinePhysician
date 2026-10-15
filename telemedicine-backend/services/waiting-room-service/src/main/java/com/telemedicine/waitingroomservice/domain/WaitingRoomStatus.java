package com.telemedicine.waitingroomservice.domain;

public enum WaitingRoomStatus {
    OPEN_PATIENT,
    CHECKED_IN,
    OPEN_DOCTOR,
    NO_SHOW,
    TIMED_OUT;

    public static WaitingRoomStatus fromWire(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        return WaitingRoomStatus.valueOf(value.trim().toUpperCase());
    }
}
