package com.telemedicine.clinicaldocsservice.adapter;

public record MedipassCertificateRequest(String appointmentId, int durationDays, String diagnosis) {}
