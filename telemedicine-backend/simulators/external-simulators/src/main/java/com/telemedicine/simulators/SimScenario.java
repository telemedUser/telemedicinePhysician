package com.telemedicine.simulators;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class SimScenario {

    public static final String HEADER = "X-Sim-Scenario";

    private SimScenario() {}

    public static void requireSuccess(String scenario) {
        if ("failure".equalsIgnoreCase(scenario) || "error".equalsIgnoreCase(scenario)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated vendor failure");
        }
        if ("timeout".equalsIgnoreCase(scenario)) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Simulated vendor timeout");
        }
    }

    public static String deterministicId(String prefix, String seed) {
        UUID uuid = UUID.nameUUIDFromBytes((prefix + ":" + seed).getBytes(StandardCharsets.UTF_8));
        return prefix + "-" + uuid.toString().substring(0, 8);
    }

    public static String scenarioOrDefault(@RequestHeader(value = HEADER, required = false) String scenario) {
        return scenario == null ? "success" : scenario;
    }
}
