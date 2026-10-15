package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * An appointment moved between states in the waiting-room state machine.
 *
 * <p>Both the old and new states are published so a consumer that missed an earlier event can tell
 * whether it has a gap, instead of silently applying an out-of-order transition.
 *
 * @param appointmentId appointment whose state changed
 * @param fromStatus previous state, null for the initial transition
 * @param toStatus new state
 * @param actorRef pseudonymous reference to whoever caused the change
 * @param changedAt when the transition happened
 */
public record StatusChanged(
        String appointmentId,
        String fromStatus,
        String toStatus,
        String actorRef,
        Instant changedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.STATUS_CHANGED;
    }
}
