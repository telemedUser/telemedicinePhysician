package com.telemedicine.reportingservice.service;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.reportingservice.config.ReportingServiceProperties;
import com.telemedicine.reportingservice.domain.KpiFamily;
import com.telemedicine.reportingservice.domain.ReportExportEntity;
import com.telemedicine.reportingservice.domain.ReportExportRepository;
import com.telemedicine.reportingservice.export.MinioExportStorage;
import com.telemedicine.reportingservice.web.dto.ReportExportRequest;
import com.telemedicine.reportingservice.web.dto.ReportExportResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportExportService {

    private final KpiAggregationService aggregationService;
    private final ReportExportRepository exportRepository;
    private final MinioExportStorage exportStorage;
    private final ReportingServiceProperties properties;

    public ReportExportService(
            KpiAggregationService aggregationService,
            ReportExportRepository exportRepository,
            MinioExportStorage exportStorage,
            ReportingServiceProperties properties) {
        this.aggregationService = aggregationService;
        this.exportRepository = exportRepository;
        this.exportStorage = exportStorage;
        this.properties = properties;
    }

    @Transactional
    public ReportExportResponse export(ReportExportRequest request) {
        String format = request.format().trim().toUpperCase(Locale.ROOT);
        LocalDate day = request.date() != null ? request.date() : LocalDate.now(ZoneOffset.UTC);
        if (request.date() != null) {
            aggregationService.generateDailySnapshots(day);
        }
        String content = buildCsv(day);
        String objectKey =
                properties.getExportPrefix()
                        + "kpi-"
                        + day
                        + "-"
                        + IdGenerator.newUlid()
                        + ".csv";
        MinioExportStorage.StoredObject stored =
                exportStorage.storeText(objectKey, contentType(format), content);

        ReportExportEntity entity = new ReportExportEntity();
        entity.setId(IdGenerator.newUlid());
        entity.setFormat(format);
        entity.setObjectKey(stored.objectKey());
        entity.setDownloadUrl(stored.downloadUrl());
        entity.setExpiresAt(stored.expiresAt());
        entity.setCreatedAt(Instant.now());
        exportRepository.save(entity);

        return new ReportExportResponse(
                entity.getId(), entity.getFormat(), entity.getDownloadUrl(), entity.getExpiresAt());
    }

    private String buildCsv(LocalDate day) {
        StringBuilder builder = new StringBuilder("family,metric,value\n");
        for (KpiFamily family : KpiFamily.values()) {
            Map<String, Double> metrics = aggregationService.currentMetrics(family);
            metrics.forEach(
                    (metric, value) ->
                            builder.append(family.name().toLowerCase())
                                    .append(',')
                                    .append(metric)
                                    .append(',')
                                    .append(value == null ? "" : value)
                                    .append('\n'));
        }
        builder.append("# snapshotDate=").append(day).append('\n');
        return builder.toString();
    }

    private static String contentType(String format) {
        return "EXCEL".equals(format) || "XLSX".equals(format)
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "text/csv";
    }
}
