package com.telemedicine.common.events.consumer;

import java.time.Instant;

/** A row in the processed-events ledger. */
public record ProcessedEventRecord(
        String consumerGroup, String eventId, String eventType, Instant processedAt) {}
