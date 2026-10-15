package com.telemedicine.simulators.admin;

import com.telemedicine.simulators.engine.SimulatorEngine;
import com.telemedicine.simulators.engine.VendorConfig;
import com.telemedicine.simulators.engine.VendorNames;
import com.telemedicine.simulators.state.AppointmentStateStore;
import com.telemedicine.simulators.state.FonasaChallengeStore;
import com.telemedicine.simulators.state.NotifyDeliveryLog;
import com.telemedicine.simulators.state.ProviderOutageState;
import com.telemedicine.simulators.state.VideoSessionStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/sim/admin")
public class SimulatorAdminController {

    private final SimulatorEngine engine;
    private final AppointmentStateStore appointmentStateStore;
    private final FonasaChallengeStore fonasaChallengeStore;
    private final VideoSessionStore videoSessionStore;
    private final NotifyDeliveryLog notifyDeliveryLog;
    private final ProviderOutageState providerOutageState;

    public SimulatorAdminController(
            SimulatorEngine engine,
            AppointmentStateStore appointmentStateStore,
            FonasaChallengeStore fonasaChallengeStore,
            VideoSessionStore videoSessionStore,
            NotifyDeliveryLog notifyDeliveryLog,
            ProviderOutageState providerOutageState) {
        this.engine = engine;
        this.appointmentStateStore = appointmentStateStore;
        this.fonasaChallengeStore = fonasaChallengeStore;
        this.videoSessionStore = videoSessionStore;
        this.notifyDeliveryLog = notifyDeliveryLog;
        this.providerOutageState = providerOutageState;
    }

    @GetMapping("/vendors")
    public Map<String, VendorConfig> listVendors() {
        return engine.allConfigs();
    }

    @GetMapping("/vendors/{vendor}")
    public VendorConfig getVendor(@PathVariable String vendor) {
        return engine.getConfig(normalizeVendor(vendor));
    }

    @PutMapping("/vendors/{vendor}")
    public VendorConfig updateVendor(@PathVariable String vendor, @RequestBody VendorConfigUpdate update) {
        String normalized = normalizeVendor(vendor);
        VendorConfig current = engine.getConfig(normalized);
        VendorConfig updated =
                new VendorConfig(
                        update.latencyMs() != null ? update.latencyMs() : current.latencyMs(),
                        update.latencyJitterMs() != null
                                ? update.latencyJitterMs()
                                : current.latencyJitterMs(),
                        update.errorRate() != null ? update.errorRate() : current.errorRate(),
                        update.timeoutRate() != null ? update.timeoutRate() : current.timeoutRate(),
                        update.scenario() != null ? update.scenario() : current.scenario(),
                        update.enabled() != null ? update.enabled() : current.enabled());
        engine.updateConfig(normalized, updated);
        return engine.getConfig(normalized);
    }

    @PostMapping("/vendors/{vendor}/reset")
    public VendorConfig resetVendor(@PathVariable String vendor) {
        String normalized = normalizeVendor(vendor);
        engine.resetVendor(normalized);
        return engine.getConfig(normalized);
    }

    @PostMapping("/reset")
    public Map<String, VendorConfig> resetAll() {
        engine.resetAll();
        return engine.allConfigs();
    }

    @PostMapping("/scenarios/chaos")
    public Map<String, VendorConfig> chaos() {
        engine.applyChaosMode();
        return engine.allConfigs();
    }

    @PostMapping("/scenarios/happy")
    public Map<String, VendorConfig> happy() {
        engine.applyHappyMode();
        providerOutageState.clear();
        return engine.allConfigs();
    }

    @PostMapping("/scenarios/resilience")
    public Map<String, VendorConfig> resilience() {
        engine.applyResilienceMode();
        return engine.allConfigs();
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("appointments", appointmentStateStore.snapshot());
        state.put("fonasaChallenges", fonasaChallengeStore.snapshot());
        state.put("videoSessions", videoSessionStore.snapshot());
        state.put("notifications", notifyDeliveryLog.recent());
        state.put("outages", providerOutageState.snapshot());
        state.put("vendors", engine.allConfigs());
        return state;
    }

    @DeleteMapping("/state")
    public Map<String, String> clearState() {
        appointmentStateStore.clear();
        fonasaChallengeStore.clear();
        videoSessionStore.clear();
        notifyDeliveryLog.clear();
        providerOutageState.clear();
        return Map.of("status", "CLEARED");
    }

    @GetMapping("/notifications")
    public List<NotifyDeliveryLog.DeliveryEntry> notifications(
            @RequestParam(required = false) String appointmentId) {
        if (appointmentId != null && !appointmentId.isBlank()) {
            return notifyDeliveryLog.byAppointmentId(appointmentId);
        }
        return notifyDeliveryLog.recent();
    }

    public record VendorConfigUpdate(
            Long latencyMs,
            Long latencyJitterMs,
            Double errorRate,
            Double timeoutRate,
            String scenario,
            Boolean enabled) {}

    private static String normalizeVendor(String vendor) {
        for (String known : VendorNames.ALL) {
            if (known.equalsIgnoreCase(vendor)) {
                return known;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown vendor: " + vendor);
    }
}
