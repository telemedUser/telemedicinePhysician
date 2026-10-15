package com.telemedicine.common.persistence.processed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @EmbeddedId private ProcessedEventId id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant processedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    public ProcessedEventId getId() {
        return id;
    }

    public void setId(ProcessedEventId id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Embeddable
    public static class ProcessedEventId implements Serializable {

        @Column(name = "consumer_group", nullable = false, length = 128)
        private String consumerGroup;

        @Column(name = "event_id", nullable = false, length = 64)
        private String eventId;

        public ProcessedEventId() {}

        public ProcessedEventId(String consumerGroup, String eventId) {
            this.consumerGroup = consumerGroup;
            this.eventId = eventId;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ProcessedEventId that)) {
                return false;
            }
            return Objects.equals(consumerGroup, that.consumerGroup)
                    && Objects.equals(eventId, that.eventId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(consumerGroup, eventId);
        }
    }
}
