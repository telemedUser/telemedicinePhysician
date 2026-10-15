package com.telemedicine.simulators.state;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class NotifyDeliveryLog {

    private static final int MAX_ENTRIES = 100;

    public record DeliveryEntry(
            String channel,
            String messageId,
            String status,
            String appointmentId,
            String recipientRef,
            Instant sentAt) {}

    private final CopyOnWriteArrayList<DeliveryEntry> entries = new CopyOnWriteArrayList<>();

    public void append(DeliveryEntry entry) {
        entries.add(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }

    public List<DeliveryEntry> recent() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<DeliveryEntry> byAppointmentId(String appointmentId) {
        return entries.stream()
                .filter(entry -> appointmentId.equals(entry.appointmentId()))
                .toList();
    }

    public void clear() {
        entries.clear();
    }
}
