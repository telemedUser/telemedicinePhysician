package com.telemedicine.reportingservice.web.dto;

import java.time.LocalDate;

public record ReportExportRequest(String format, LocalDate date) {}
