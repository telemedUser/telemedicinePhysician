package com.telemedicine.reportingservice.domain;

public enum KpiFamily {
    TECHNICAL,
    FUNCTIONAL,
    BUSINESS;

    public static KpiFamily fromPath(String value) {
        return KpiFamily.valueOf(value.trim().toUpperCase());
    }
}
