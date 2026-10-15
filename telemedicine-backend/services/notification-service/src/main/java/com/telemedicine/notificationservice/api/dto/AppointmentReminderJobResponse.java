package com.telemedicine.notificationservice.api.dto;

public record AppointmentReminderJobResponse(int processed, int sent, int skipped, boolean degraded) {}
