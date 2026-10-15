package com.telemedicine.adminbff.dto;

import com.telemedicine.common.audit.AuditEvent;
import java.util.List;

public record AuditQueryResponse(List<AuditEvent> events) {}
