package com.telemedicine.simulators.state;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AppointmentStateStore {

    private static final Map<String, java.util.Set<String>> ALLOWED_TRANSITIONS =
            Map.of(
                    "SCHEDULED", java.util.Set.of("IN_PROGRESS", "CHECKED_IN", "CANCELLED"),
                    "IN_PROGRESS", java.util.Set.of("COMPLETED", "CANCELLED"),
                    "CHECKED_IN", java.util.Set.of("OPEN_DOCTOR", "IN_PROGRESS", "COMPLETED", "CANCELLED"),
                    "OPEN_DOCTOR", java.util.Set.of("IN_PROGRESS", "COMPLETED"),
                    "COMPLETED", java.util.Set.of(),
                    "CANCELLED", java.util.Set.of());

    private final ConcurrentHashMap<String, AppointmentState> appointments = new ConcurrentHashMap<>();

    public AppointmentState getOrCreate(String appointmentId) {
        return appointments.computeIfAbsent(
                appointmentId,
                id ->
                        new AppointmentState(
                                id,
                                "SCHEDULED",
                                "UNPAID",
                                "PENDING",
                                null,
                                Instant.now()));
    }

    public AppointmentState get(String appointmentId) {
        return appointments.get(appointmentId);
    }

    public boolean exists(String appointmentId) {
        return appointments.containsKey(appointmentId);
    }

    public AppointmentState transition(String appointmentId, String newStatus) {
        AppointmentState current = getOrCreate(appointmentId);
        java.util.Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(current.status(), java.util.Set.of());
        if (!allowed.contains(newStatus) && !current.status().equals(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid status transition from "
                            + current.status()
                            + " to "
                            + newStatus);
        }
        AppointmentState updated = current.withStatus(newStatus);
        appointments.put(appointmentId, updated);
        return updated;
    }

    public AppointmentState markPaid(String appointmentId) {
        AppointmentState current = getOrCreate(appointmentId);
        AppointmentState updated = current.withPaymentStatus("PAID");
        appointments.put(appointmentId, updated);
        return updated;
    }

    public AppointmentState markLinked(String appointmentId) {
        AppointmentState current = getOrCreate(appointmentId);
        AppointmentState updated = current.withLinkedAt(Instant.now());
        appointments.put(appointmentId, updated);
        return updated;
    }

    public Map<String, AppointmentState> snapshot() {
        return Map.copyOf(appointments);
    }

    public void clear() {
        appointments.clear();
    }
}
