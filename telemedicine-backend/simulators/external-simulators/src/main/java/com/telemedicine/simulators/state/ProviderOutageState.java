package com.telemedicine.simulators.state;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class ProviderOutageState {

    private final AtomicBoolean vonageOutage = new AtomicBoolean(false);
    private final AtomicBoolean acsOutage = new AtomicBoolean(false);

    public boolean isVonageOutage() {
        return vonageOutage.get();
    }

    public boolean setVonageOutage(boolean enabled) {
        vonageOutage.set(enabled);
        return vonageOutage.get();
    }

    public boolean isAcsOutage() {
        return acsOutage.get();
    }

    public boolean setAcsOutage(boolean enabled) {
        acsOutage.set(enabled);
        return acsOutage.get();
    }

    public java.util.Map<String, Boolean> snapshot() {
        return java.util.Map.of("vonageOutage", vonageOutage.get(), "acsOutage", acsOutage.get());
    }

    public void clear() {
        vonageOutage.set(false);
        acsOutage.set(false);
    }
}
