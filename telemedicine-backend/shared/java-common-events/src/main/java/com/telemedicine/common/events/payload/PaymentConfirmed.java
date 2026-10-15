package com.telemedicine.common.events.payload;

import com.telemedicine.common.events.EventTypes;
import java.time.Instant;

/**
 * Payment for an appointment cleared, which is what unlocks the secure access link.
 *
 * @param appointmentId appointment the payment settles
 * @param paymentId identifier in the payment system
 * @param amountMinor amount in the currency's minor unit, avoiding floating point on money
 * @param currency ISO 4217 code
 * @param method coded payment method
 * @param confirmedAt when the payment system confirmed
 */
public record PaymentConfirmed(
        String appointmentId,
        String paymentId,
        long amountMinor,
        String currency,
        String method,
        Instant confirmedAt)
        implements EventPayload {

    @Override
    public String eventType() {
        return EventTypes.PAYMENT_CONFIRMED;
    }
}
