package com.telemedicine.simulators.engine;

public record VendorConfig(
        long latencyMs,
        long latencyJitterMs,
        double errorRate,
        double timeoutRate,
        String scenario,
        boolean enabled) {

    public static VendorConfig defaults() {
        return new VendorConfig(0L, 0L, 0.0, 0.0, "success", true);
    }

    public VendorConfig withLatencyMs(long value) {
        return new VendorConfig(value, latencyJitterMs, errorRate, timeoutRate, scenario, enabled);
    }

    public VendorConfig withLatencyJitterMs(long value) {
        return new VendorConfig(latencyMs, value, errorRate, timeoutRate, scenario, enabled);
    }

    public VendorConfig withErrorRate(double value) {
        return new VendorConfig(latencyMs, latencyJitterMs, value, timeoutRate, scenario, enabled);
    }

    public VendorConfig withTimeoutRate(double value) {
        return new VendorConfig(latencyMs, latencyJitterMs, errorRate, value, scenario, enabled);
    }

    public VendorConfig withScenario(String value) {
        return new VendorConfig(latencyMs, latencyJitterMs, errorRate, timeoutRate, value, enabled);
    }

    public VendorConfig withEnabled(boolean value) {
        return new VendorConfig(latencyMs, latencyJitterMs, errorRate, timeoutRate, scenario, value);
    }
}
