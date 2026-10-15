package com.telemedicine.reportingservice.service;

import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.reportingservice.domain.KpiFamily;
import com.telemedicine.reportingservice.web.dto.KpiFamilyResponse;
import com.telemedicine.reportingservice.web.dto.KpiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {

    private final KpiAggregationService aggregationService;

    public ReportingService(KpiAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    public KpiResponse allKpis() {
        Map<String, Object> families = new LinkedHashMap<>();
        for (KpiFamily family : KpiFamily.values()) {
            families.put(family.name().toLowerCase(), familyMetrics(family));
        }
        return new KpiResponse(families);
    }

    public KpiFamilyResponse familyKpis(String familyPath) {
        KpiFamily family;
        try {
            family = KpiFamily.fromPath(familyPath);
        } catch (IllegalArgumentException exception) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Unknown KPI family: " + familyPath);
        }
        return new KpiFamilyResponse(family.name().toLowerCase(), familyMetrics(family));
    }

    private Map<String, Object> familyMetrics(KpiFamily family) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        aggregationService.currentMetrics(family).forEach(metrics::put);
        if (family == KpiFamily.BUSINESS && !metrics.containsKey("telemedicine.journey.physician.satisfaction")) {
            metrics.put("telemedicine.journey.physician.satisfaction", null);
        }
        return metrics;
    }
}
