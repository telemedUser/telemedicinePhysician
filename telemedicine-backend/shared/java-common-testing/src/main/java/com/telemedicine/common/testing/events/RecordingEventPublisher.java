package com.telemedicine.common.testing.events;

import com.telemedicine.common.events.EventEnvelope;
import com.telemedicine.common.events.EventPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory {@link EventPublisher} for unit tests that need to assert on emitted events without a
 * broker.
 */
public final class RecordingEventPublisher implements EventPublisher {

    private final CopyOnWriteArrayList<EventEnvelope<?>> published = new CopyOnWriteArrayList<>();

    @Override
    public <T> CompletableFuture<Void> publish(EventEnvelope<T> envelope) {
        published.add(envelope);
        return CompletableFuture.completedFuture(null);
    }

    public List<EventEnvelope<?>> all() {
        return List.copyOf(published);
    }

    public List<EventEnvelope<?>> ofType(String eventType) {
        List<EventEnvelope<?>> matches = new ArrayList<>();
        for (EventEnvelope<?> envelope : published) {
            if (eventType.equals(envelope.type())) {
                matches.add(envelope);
            }
        }
        return matches;
    }

    public void clear() {
        published.clear();
    }
}
