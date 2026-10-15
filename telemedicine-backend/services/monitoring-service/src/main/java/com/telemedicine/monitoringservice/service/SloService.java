package com.telemedicine.monitoringservice.service;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.monitoringservice.config.MonitoringServiceProperties;
import com.telemedicine.monitoringservice.domain.SloWindowEntity;
import com.telemedicine.monitoringservice.domain.SloWindowRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SloService {

    private final SloWindowRepository sloWindowRepository;
    private final MonitoringServiceProperties properties;

    public SloService(SloWindowRepository sloWindowRepository, MonitoringServiceProperties properties) {
        this.sloWindowRepository = sloWindowRepository;
        this.properties = properties;
    }

    @Transactional
    public void recordRequest() {
        SloWindowEntity window = currentWindow();
        window.setTotalRequests(window.getTotalRequests() + 1);
        window.setBurnRate(computeBurnRate(window));
        window.setUpdatedAt(Instant.now());
        sloWindowRepository.save(window);
    }

    @Transactional
    public void recordError() {
        SloWindowEntity window = currentWindow();
        window.setTotalRequests(window.getTotalRequests() + 1);
        window.setErrorCount(window.getErrorCount() + 1);
        window.setBurnRate(computeBurnRate(window));
        window.setUpdatedAt(Instant.now());
        sloWindowRepository.save(window);
    }

    public double currentErrorRate() {
        return sloWindowRepository
                .findFirstByOrderByWindowEndDesc()
                .map(window -> window.getTotalRequests() == 0
                        ? 0.0
                        : (double) window.getErrorCount() / window.getTotalRequests())
                .orElse(0.0);
    }

    public Double currentBurnRate() {
        return sloWindowRepository.findFirstByOrderByWindowEndDesc().map(SloWindowEntity::getBurnRate).orElse(0.0);
    }

    private SloWindowEntity currentWindow() {
        Instant now = Instant.now();
        return sloWindowRepository
                .findFirstByOrderByWindowEndDesc()
                .filter(window -> now.isBefore(window.getWindowEnd()))
                .orElseGet(() -> createWindow(now));
    }

    private SloWindowEntity createWindow(Instant now) {
        SloWindowEntity entity = new SloWindowEntity();
        entity.setId(IdGenerator.newUlid());
        entity.setWindowStart(now.truncatedTo(ChronoUnit.MINUTES));
        entity.setWindowEnd(now.plus(properties.getSloWindowMinutes(), ChronoUnit.MINUTES));
        entity.setTotalRequests(0);
        entity.setErrorCount(0);
        entity.setBurnRate(0.0);
        entity.setUpdatedAt(now);
        return sloWindowRepository.save(entity);
    }

    private static double computeBurnRate(SloWindowEntity window) {
        if (window.getTotalRequests() == 0) {
            return 0.0;
        }
        return (double) window.getErrorCount() / window.getTotalRequests();
    }
}
