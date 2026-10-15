package com.telemedicine.simulators.engine;

import com.telemedicine.simulators.SimScenario;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SimulatorEngine {

    private static final long TIMEOUT_SLEEP_MS = 31_000L;

    private final ConcurrentHashMap<String, VendorConfig> runtimeConfigs = new ConcurrentHashMap<>();
    private final Map<String, VendorConfig> yamlDefaults;

    public SimulatorEngine(SimulatorProperties properties) {
        this.yamlDefaults = new LinkedHashMap<>(properties.getVendors());
        for (Map.Entry<String, VendorConfig> entry : yamlDefaults.entrySet()) {
            runtimeConfigs.put(entry.getKey(), entry.getValue());
        }
    }

    public ProcessOutcome preProcess(String vendor, String scenarioHeader) {
        String headerScenario = normalize(scenarioHeader);
        if (headerScenario != null) {
            ProcessOutcome headerOutcome = applyExplicitScenario(headerScenario);
            if (headerOutcome != null) {
                applyLatency(getConfig(vendor));
                return headerOutcome;
            }
            if (isVendorSpecificScenario(headerScenario)) {
                ensureEnabled(vendor);
                applyLatency(getConfig(vendor));
                return ProcessOutcome.SUCCESS;
            }
        }

        VendorConfig config = getConfig(vendor);
        ensureEnabled(vendor);

        if (shouldApplyProbabilisticBehavior(headerScenario)) {
            if (roll(config.timeoutRate())) {
                simulateTimeout();
            }
            if (roll(config.errorRate())) {
                throw failureException();
            }
        }

        applyLatency(config);
        return ProcessOutcome.SUCCESS;
    }

    public VendorConfig getConfig(String vendor) {
        return runtimeConfigs.getOrDefault(vendor, VendorConfig.defaults());
    }

    public Map<String, VendorConfig> allConfigs() {
        Map<String, VendorConfig> snapshot = new LinkedHashMap<>();
        for (String vendor : VendorNames.ALL) {
            snapshot.put(vendor, getConfig(vendor));
        }
        return snapshot;
    }

    public void updateConfig(String vendor, VendorConfig config) {
        validateVendor(vendor);
        runtimeConfigs.put(vendor, config);
    }

    public void resetVendor(String vendor) {
        validateVendor(vendor);
        runtimeConfigs.put(vendor, yamlDefaults.getOrDefault(vendor, VendorConfig.defaults()));
    }

    public void resetAll() {
        runtimeConfigs.clear();
        runtimeConfigs.putAll(yamlDefaults);
    }

    public void applyChaosMode() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (String vendor : VendorNames.ALL) {
            VendorConfig current = getConfig(vendor);
            runtimeConfigs.put(
                    vendor,
                    current.withLatencyMs(100 + random.nextLong(401))
                            .withLatencyJitterMs(50)
                            .withErrorRate(0.10)
                            .withTimeoutRate(0.0)
                            .withScenario("success")
                            .withEnabled(true));
        }
    }

    public void applyHappyMode() {
        resetAll();
    }

    public void applyResilienceMode() {
        for (String vendor : VendorNames.ALL) {
            VendorConfig defaults = yamlDefaults.getOrDefault(vendor, VendorConfig.defaults());
            runtimeConfigs.put(vendor, defaults);
        }
        runtimeConfigs.put(
                VendorNames.VONAGE,
                getConfig(VendorNames.VONAGE).withErrorRate(1.0).withEnabled(true));
        runtimeConfigs.put(
                VendorNames.PAYMENT,
                getConfig(VendorNames.PAYMENT).withErrorRate(0.20).withEnabled(true));
        runtimeConfigs.put(
                VendorNames.HIS,
                getConfig(VendorNames.HIS).withLatencyMs(200).withLatencyJitterMs(0).withEnabled(true));
    }

    public boolean isPartialScenario(String scenarioHeader) {
        return "partial".equalsIgnoreCase(normalize(scenarioHeader));
    }

    public static boolean isVendorSpecificScenario(String scenario) {
        if (scenario == null) {
            return false;
        }
        return switch (scenario.toLowerCase()) {
            case "not-found",
                    "stale-status",
                    "invalid-appointment",
                    "duplicate-link",
                    "unpaid",
                    "payment-pending",
                    "locked-out",
                    "invalid-rut",
                    "session-expired",
                    "capacity-exceeded",
                    "measurement-failed",
                    "consent-missing",
                    "validation-error",
                    "certificate-rejected",
                    "rate-limited",
                    "channel-unavailable",
                    "invalid-recipient",
                    "multiple-matches" -> true;
            default -> false;
        };
    }

    private ProcessOutcome applyExplicitScenario(String scenario) {
        if ("failure".equalsIgnoreCase(scenario) || "error".equalsIgnoreCase(scenario)) {
            throw failureException();
        }
        if ("timeout".equalsIgnoreCase(scenario)) {
            simulateTimeout();
            throw timeoutException();
        }
        if ("partial".equalsIgnoreCase(scenario)) {
            return ProcessOutcome.PARTIAL;
        }
        return null;
    }

    private void ensureEnabled(String vendor) {
        if (!getConfig(vendor).enabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Simulated vendor unavailable: " + vendor);
        }
    }

    private boolean shouldApplyProbabilisticBehavior(String headerScenario) {
        return headerScenario == null
                || "success".equalsIgnoreCase(headerScenario)
                || headerScenario.isBlank();
    }

    private void applyLatency(VendorConfig config) {
        long delay = config.latencyMs();
        if (config.latencyJitterMs() > 0) {
            delay += ThreadLocalRandom.current().nextLong(config.latencyJitterMs() + 1);
        }
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Latency interrupted");
        }
    }

    private void simulateTimeout() {
        try {
            Thread.sleep(TIMEOUT_SLEEP_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        throw timeoutException();
    }

    private static boolean roll(double rate) {
        return rate > 0.0 && ThreadLocalRandom.current().nextDouble() < rate;
    }

    private static ResponseStatusException failureException() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated vendor failure");
    }

    private static ResponseStatusException timeoutException() {
        return new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Simulated vendor timeout");
    }

    private static String normalize(String scenarioHeader) {
        if (scenarioHeader == null || scenarioHeader.isBlank()) {
            return null;
        }
        return scenarioHeader.trim();
    }

    private static void validateVendor(String vendor) {
        for (String known : VendorNames.ALL) {
            if (known.equalsIgnoreCase(vendor)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown vendor: " + vendor);
    }
}
