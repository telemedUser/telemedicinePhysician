package com.telemedicine.simulators;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SimulatorSafetyGuard {

    @EventListener(ApplicationReadyEvent.class)
    void refuseProductionStartup() {
        assertNotProduction(System.getenv("TELEMEDICINE_ENVIRONMENT"));
    }

    static void assertNotProduction(String environment) {
        if (environment != null && "production".equalsIgnoreCase(environment.trim())) {
            throw new IllegalStateException(
                    "external-simulators must not run when TELEMEDICINE_ENVIRONMENT=production");
        }
    }
}
