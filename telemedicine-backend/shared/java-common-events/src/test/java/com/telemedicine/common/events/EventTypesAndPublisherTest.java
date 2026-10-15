package com.telemedicine.common.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.telemedicine.common.events.payload.ConfigurationChanged;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class EventTypesAndPublisherTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void forPublicationLeavesVersionAtDefaultForNonPayloadTypes() {
        EventEnvelope<String> envelope =
                EventEnvelope.forPublication(EventTypes.STATUS_CHANGED, "APT-1", "body");
        assertThat(envelope.version()).isEqualTo("1.0");
    }

    @Test
    void catalogueHelpersWork() {
        assertThat(EventTypes.isKnown(EventTypes.AUDIT_RECORDED)).isTrue();
        assertThat(EventTypes.isKnown("Missing")).isFalse();
        assertThat(EventTypes.require(EventTypes.AUDIT_RECORDED))
                .isEqualTo(EventTypes.AUDIT_RECORDED);
        assertThatIllegalArgumentException().isThrownBy(() -> EventTypes.require("Missing"));
    }

    @Test
    void envelopeFactoryMethodsPopulateFields() {
        ConfigurationChanged payload =
                new ConfigurationChanged(
                        "video.provider",
                        "GLOBAL",
                        null,
                        "abc",
                        "admin",
                        Instant.parse("2026-01-01T00:00:00Z"));

        EventEnvelope<ConfigurationChanged> fromOf =
                EventEnvelope.of(
                        EventTypes.CONFIGURATION_CHANGED, "GLOBAL", payload, "01JCORR", "01JCAUSE");
        assertThat(fromOf.correlationId()).isEqualTo("01JCORR");
        assertThat(fromOf.causationId()).isEqualTo("01JCAUSE");

        MDC.put("correlationId", "01JMDC");
        MDC.put("causationId", "01JMDC2");
        EventEnvelope<ConfigurationChanged> fromPublication =
                EventEnvelope.forPublication(EventTypes.CONFIGURATION_CHANGED, "GLOBAL", payload);
        assertThat(fromPublication.correlationId()).isEqualTo("01JMDC");
        assertThat(fromPublication.version()).isEqualTo("1.0");

        EventEnvelope<String> swapped = fromOf.withPayload("other");
        assertThat(swapped.payload()).isEqualTo("other");
        assertThat(swapped.eventId()).isEqualTo(fromOf.eventId());
    }

    @Test
    void headersBuildSchemaUris() {
        assertThat(EventHeaders.schemaUri(EventTypes.LINK_GENERATED, "1.0"))
                .isEqualTo("https://telemedicine.bupa/schemas/events/LinkGenerated/1.0");
    }

    @Test
    void publisherDefaultUsesPayloadType() {
        RecordingPublisher publisher = new RecordingPublisher();
        ConfigurationChanged payload =
                new ConfigurationChanged(
                        "video.provider",
                        "GLOBAL",
                        null,
                        "abc",
                        "admin",
                        Instant.parse("2026-01-01T00:00:00Z"));

        publisher.publish("GLOBAL", payload).join();

        assertThat(publisher.last.type()).isEqualTo(EventTypes.CONFIGURATION_CHANGED);
        assertThat(publisher.last.subject()).isEqualTo("GLOBAL");
    }

    static class RecordingPublisher implements EventPublisher {
        EventEnvelope<?> last;

        @Override
        public <T> java.util.concurrent.CompletableFuture<Void> publish(EventEnvelope<T> envelope) {
            last = envelope;
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
    }
}
