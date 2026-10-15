package com.telemedicine.physicianservice.service;

import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.physicianservice.api.dto.ProviderDetailResponse;
import com.telemedicine.physicianservice.api.dto.ProviderStatusRequest;
import com.telemedicine.physicianservice.api.dto.ProviderSummaryResponse;
import com.telemedicine.physicianservice.domain.provider.ProviderEntity;
import com.telemedicine.physicianservice.domain.provider.ProviderMetricsEntity;
import com.telemedicine.physicianservice.domain.provider.ProviderMetricsRepository;
import com.telemedicine.physicianservice.domain.provider.ProviderRepository;
import com.telemedicine.physicianservice.domain.provider.ProviderStatus;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provider & physician management (PRD §21). Directory search and detail read from the market-scoped
 * provider projection; status changes take immediate effect on routing and the directory
 * (TM-PRV-003). Null search filters are ignored.
 */
@Service
public class ProviderService {

    private static final Logger log = LoggerFactory.getLogger(ProviderService.class);

    private final ProviderRepository providerRepository;
    private final ProviderMetricsRepository metricsRepository;

    public ProviderService(
            ProviderRepository providerRepository, ProviderMetricsRepository metricsRepository) {
        this.providerRepository = providerRepository;
        this.metricsRepository = metricsRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderSummaryResponse> search(
            String market, String status, String specialty, String mode, String language) {
        String normalizedStatus =
                status == null || status.isBlank()
                        ? null
                        : ProviderStatus.fromWire(status).name();
        String specialtyLike =
                specialty == null || specialty.isBlank()
                        ? null
                        : "%" + specialty.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        return providerRepository
                .search(market, normalizedStatus, specialtyLike, blankToNull(mode), blankToNull(language))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderDetailResponse detail(String providerId, String market) {
        ProviderEntity provider =
                providerRepository
                        .findByIdAndMarket(providerId, market)
                        .orElseThrow(() -> NotFoundException.of("provider", providerId));
        ProviderMetricsEntity metrics = metricsRepository.findById(providerId).orElse(null);
        return toDetail(provider, metrics);
    }

    /** Change the provider's operational status (TM-PRV-003). Immediate effect on routing/directory. */
    @Transactional
    public ProviderDetailResponse changeStatus(
            String providerId, String market, ProviderStatusRequest request) {
        ProviderEntity provider =
                providerRepository
                        .findByIdAndMarket(providerId, market)
                        .orElseThrow(() -> NotFoundException.of("provider", providerId));
        ProviderStatus target = ProviderStatus.fromWire(request.status());
        provider.setStatus(target);
        providerRepository.save(provider);
        log.info(
                "Provider {} status changed to {} ({})",
                providerId,
                target,
                request.reason() == null ? "no reason" : "reason recorded");
        return toDetail(provider, metricsRepository.findById(providerId).orElse(null));
    }

    // --- mapping ---

    private ProviderSummaryResponse toSummary(ProviderEntity p) {
        return new ProviderSummaryResponse(
                p.getId(),
                p.getDisplayName(),
                p.getStatus().name(),
                p.getSpecialties().stream().map(s -> s.getSpecialty()).toList(),
                p.getLanguages().stream().map(l -> l.getLanguage()).toList(),
                p.getModes().stream().map(m -> m.getMode()).toList());
    }

    private ProviderDetailResponse toDetail(ProviderEntity p, ProviderMetricsEntity m) {
        List<ProviderDetailResponse.Credential> creds =
                p.getCredentials().stream()
                        .map(
                                c ->
                                        new ProviderDetailResponse.Credential(
                                                c.getType(),
                                                c.getNumber(),
                                                c.getValidUntil() == null ? null : c.getValidUntil().toString(),
                                                c.isVerified()))
                        .toList();
        List<ProviderDetailResponse.AvailabilityWindow> windows =
                p.getAvailability().stream()
                        .map(
                                a ->
                                        new ProviderDetailResponse.AvailabilityWindow(
                                                a.getDayOfWeek(),
                                                a.getStartTime().toString(),
                                                a.getEndTime().toString(),
                                                a.getMode(),
                                                a.getMaxConcurrent()))
                        .toList();
        ProviderDetailResponse.Metrics metrics =
                m == null
                        ? new ProviderDetailResponse.Metrics(0, 0, 0, 0, 0)
                        : new ProviderDetailResponse.Metrics(
                                m.getUtilization(),
                                m.getAvgDurationSeconds(),
                                m.getAvgDocumentationSeconds(),
                                m.getNoShowRate(),
                                m.getTechnicalFailureRate());
        return new ProviderDetailResponse(
                p.getId(),
                p.getDisplayName(),
                p.getStatus().name(),
                p.getSpecialties().stream().map(s -> s.getSpecialty()).toList(),
                p.getLanguages().stream().map(l -> l.getLanguage()).toList(),
                p.getModes().stream().map(mode -> mode.getMode()).toList(),
                creds,
                windows,
                metrics,
                p.getSourceRef(),
                Optional.ofNullable(p.getLastRefreshedAt())
                        .map(DateTimeFormatter.ISO_INSTANT::format)
                        .orElse(null),
                p.isStale());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
